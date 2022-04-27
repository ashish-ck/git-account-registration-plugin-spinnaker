package com.google.git.spinnaker.plugin.registration;

import com.netflix.spinnaker.kork.secrets.SecretManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GitAccountsStatusTest {

    private GitAccountsStatus gitAccountsStatus;

    @BeforeEach
    public void setUp() {
        String accountsYml = "google:\n" +
                "  enabled: true\n" +
                "  accounts:\n" +
                "  - name: spinnaker-gce-account-v1.2\n" +
                "    jsonPath: encryptedFile:gcs!b:gce-accounts-v1!f:gce-account.json\n" +
                "    consul:\n" +
                "      enabled: false\n" +
                "  primaryAccount: spinnaker-gce-account-v1.2\n" +
                "  bakeryDefaults:\n" +
                "    useInternalIp: false";
        String gitHttpsUsername = "git_username";
        String gitHttpsPassword = "password";
        String githubOAuthAccessToken = "ghp_5b58J65pqQDbKdsdrgDT9iq7pDPI3uciFh";
        String sshPrivateKeyFilePath = "~/.ssh/id_ed25519";
        String sshPrivateKeyPassphrase = "paraphrase";
        String sshKnownHostsFilePath = "~/.ssh/known_hosts";
        Boolean sshTrustUnknownHosts = true;
        gitAccountsStatus = spy(new GitAccountsStatus(gitHttpsUsername, gitHttpsPassword,
                githubOAuthAccessToken, sshPrivateKeyFilePath, sshPrivateKeyPassphrase, sshKnownHostsFilePath,
                sshTrustUnknownHosts));
        ReflectionTestUtils.setField(gitAccountsStatus, "repositoryName", "https://test.git");
        ReflectionTestUtils.setField(gitAccountsStatus, "filename", "accounts.yml");
        SecretManager secretManager = spy(mock(SecretManager.class));
        ReflectionTestUtils.setField(gitAccountsStatus, "secretManager", secretManager);
        ReflectionTestUtils.setField(gitAccountsStatus, "credentialType", GitAccountsStatus.GitCredentialType.NONE);
        doReturn(new ByteArrayInputStream(accountsYml.getBytes())).when(gitAccountsStatus).downloadRemoteFile();
        doReturn(mock(Path.class)).when(secretManager).decryptAsFile(anyString());
    }

    @Test
    public void testFetchAccounts() {
        assertTrue(gitAccountsStatus.fetchAccounts());
    }

}