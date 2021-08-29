/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
// This should be updated manually.
// See: https://www.jenkins.io/changelog-stable/
def recentLts = '2.303.1'

buildPlugin(
  configurations: [
    [ platform: 'linux', jdk: '8' ],
    [ platform: 'windows', jdk: '8' ],
    [ platform: 'linux', jdk: '11', jenkins: recentLts ],
  ],
  useContainerAgent: true,
)
