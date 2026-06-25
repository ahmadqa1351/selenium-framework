// ============================================================
// Enterprise Selenium Framework — Jenkinsfile
// Declarative Pipeline for CI/CD integration
//
// Stages:
//   1. Checkout       — pull source code
//   2. Build          — compile and validate the framework
//   3. Smoke Tests    — post-deploy gate (fails fast)
//   4. Regression     — full nightly suite (parallel)
//   5. Publish Report — archive HTML report and screenshots
//   6. Notify         — Slack/email notification
//
// Trigger:
//   - Smoke:      every merge to main/develop
//   - Regression: nightly at 02:00
// ============================================================

pipeline {

    agent {
        // Use a Docker agent with Chrome pre-installed
        docker {
            image 'seleniarm/standalone-chromium:latest'
            args  '--shm-size=2g -u root'
            reuseNode true
        }
    }

    // ── Parameters (overridable from Jenkins UI or API) ──
    parameters {
        choice(name: 'ENV',     choices: ['qa', 'dev', 'uat'], description: 'Target environment')
        choice(name: 'BROWSER', choices: ['chrome_headless', 'chrome', 'firefox'],
               description: 'Browser for test execution')
        choice(name: 'SUITE',   choices: ['smoke', 'regression', 'parallel'],
               description: 'TestNG suite profile to run')
        string(name: 'THREAD_COUNT', defaultValue: '4',
               description: 'Parallel thread count (regression/parallel profiles)')
    }

    environment {
        // Credentials stored in Jenkins Credential Store
        QA_ADMIN_PASSWORD = credentials('qa-admin-password')
        DB_PASSWORD       = credentials('qa-db-password')
        API_KEY           = credentials('qa-api-key')
        SLACK_WEBHOOK     = credentials('slack-webhook-url')

        // Build metadata
        REPORT_DIR   = "reports"
        TIMESTAMP    = sh(script: "date +%Y%m%d_%H%M%S", returnStdout: true).trim()
        DISPLAY      = ':99'  // Headless display for non-headless browser modes
    }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    triggers {
        // Nightly regression at 02:00 (only on main branch)
        cron(env.BRANCH_NAME == 'main' ? '0 2 * * *' : '')
    }

    stages {

        // ── Stage 1: Checkout ────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log --oneline -5'
                echo "Branch: ${env.BRANCH_NAME} | Build: ${env.BUILD_NUMBER}"
            }
        }

        // ── Stage 2: Build & Compile ─────────────────────────────
        stage('Build') {
            steps {
                echo "Compiling framework..."
                sh 'mvn clean compile test-compile -q'
            }
            post {
                failure {
                    error("Build failed — compilation errors prevent test execution.")
                }
            }
        }

        // ── Stage 3: Smoke Tests (CI gate) ───────────────────────
        stage('Smoke Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch 'release/*'
                    expression { params.SUITE == 'smoke' }
                }
            }
            steps {
                echo "Running Smoke Suite on ${params.ENV} with ${params.BROWSER}..."
                sh """
                    mvn test \
                        -P smoke \
                        -Denv=${params.ENV} \
                        -Dbrowser=${params.BROWSER} \
                        -Dadmin.password=${QA_ADMIN_PASSWORD} \
                        -Dapi.key=${API_KEY} \
                        -Dsurf.headless=true \
                        -q
                """
            }
            post {
                always {
                    archiveArtifacts artifacts: 'reports/**/*', allowEmptyArchive: true
                    publishHTML([
                        allowMissing:         false,
                        alwaysLinkToLastBuild: true,
                        keepAll:              true,
                        reportDir:            'reports',
                        reportFiles:          'extent-report_*.html',
                        reportName:           'Smoke Test Report'
                    ])
                }
                failure {
                    // Smoke failure blocks downstream — notify immediately
                    slackSend(
                        channel: '#qa-alerts',
                        color:   'danger',
                        message: "🔴 SMOKE TESTS FAILED | Build #${env.BUILD_NUMBER} | " +
                                 "Branch: ${env.BRANCH_NAME} | ENV: ${params.ENV}\n" +
                                 "Details: ${env.BUILD_URL}"
                    )
                    error("Smoke tests failed — regression will not run.")
                }
            }
        }

        // ── Stage 4: Regression Tests ────────────────────────────
        stage('Regression Tests') {
            when {
                anyOf {
                    // Run regression on nightly trigger or explicit parameter
                    triggeredBy 'TimerTrigger'
                    expression { params.SUITE == 'regression' || params.SUITE == 'parallel' }
                }
            }
            steps {
                echo "Running Regression Suite (parallel, ${params.THREAD_COUNT} threads)..."
                sh """
                    mvn test \
                        -P ${params.SUITE} \
                        -Denv=${params.ENV} \
                        -Dbrowser=${params.BROWSER} \
                        -Dparallel.thread.count=${params.THREAD_COUNT} \
                        -Dadmin.password=${QA_ADMIN_PASSWORD} \
                        -Ddb.password=${DB_PASSWORD} \
                        -Dapi.key=${API_KEY} \
                        -q
                """
            }
        }

        // ── Stage 5: Publish Reports ─────────────────────────────
        stage('Publish Reports') {
            steps {
                echo "Publishing test reports..."
                archiveArtifacts artifacts: 'reports/**/*', allowEmptyArchive: true

                publishHTML([
                    allowMissing:         false,
                    alwaysLinkToLastBuild: true,
                    keepAll:              true,
                    reportDir:            'reports',
                    reportFiles:          'extent-report_*.html',
                    reportName:           'Extent Test Report'
                ])

                // Archive TestNG XML results for trend analysis
                junit allowEmptyResults: true,
                      testResults: 'target/surefire-reports/**/*.xml'
            }
        }
    }

    // ── Post-pipeline actions ────────────────────────────────────
    post {
        success {
            slackSend(
                channel: '#qa-results',
                color:   'good',
                message: "✅ Tests PASSED | Build #${env.BUILD_NUMBER} | " +
                         "Suite: ${params.SUITE} | ENV: ${params.ENV} | " +
                         "Browser: ${params.BROWSER}\n${env.BUILD_URL}"
            )
        }
        failure {
            slackSend(
                channel: '#qa-alerts',
                color:   'danger',
                message: "🔴 Tests FAILED | Build #${env.BUILD_NUMBER} | " +
                         "Suite: ${params.SUITE} | ENV: ${params.ENV}\n${env.BUILD_URL}"
            )
            emailext(
                to:          'qa-team@company.com',
                subject:     "FAILED: Automation Build #${env.BUILD_NUMBER} — ${env.BRANCH_NAME}",
                body:        "Test execution failed. See report: ${env.BUILD_URL}",
                attachmentsPattern: 'reports/extent-report_*.html'
            )
        }
        unstable {
            slackSend(
                channel: '#qa-results',
                color:   'warning',
                message: "⚠️ Tests UNSTABLE (some failures) | Build #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }
        always {
            // Clean workspace except reports (archived above)
            cleanWs(patterns: [[pattern: 'reports/**', type: 'EXCLUDE']])
        }
    }
}
