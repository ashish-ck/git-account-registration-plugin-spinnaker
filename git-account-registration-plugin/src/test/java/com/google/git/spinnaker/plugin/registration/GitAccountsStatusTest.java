package com.google.git.spinnaker.plugin.registration;

import com.netflix.spinnaker.kork.secrets.SecretManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GitAccountsStatusTest {

    @Test
    public void testFetchAccounts() {
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
        String gitHttpsUsername = "opsmx";
        String gitHttpsPassword = "S3cret";
        String githubOAuthAccessToken = "ghp_5b58J65pqQhfDbKdsdrgDT9iq7pDPI3uciFh";
        String sshPrivateKeyFilePath = "/home/opsmx/.ssh/id_ed25519";
        String sshPrivateKeyPassphrase = "";
        String sshKnownHostsFilePath = "/home/opsmx/.ssh/known_hosts";
        Boolean sshTrustUnknownHosts = true;
        GitAccountsStatus gitAccountsStatus = spy(new GitAccountsStatus(gitHttpsUsername, gitHttpsPassword,
                githubOAuthAccessToken, sshPrivateKeyFilePath, sshPrivateKeyPassphrase, sshKnownHostsFilePath,
                sshTrustUnknownHosts));
        ReflectionTestUtils.setField(gitAccountsStatus, "repositoryName", "https://test.git");
        ReflectionTestUtils.setField(gitAccountsStatus, "filename", "accounts.yml");
        SecretManager secretManager = spy(mock(SecretManager.class));
        ReflectionTestUtils.setField(gitAccountsStatus, "secretManager", secretManager);
        ReflectionTestUtils.setField(gitAccountsStatus, "credentialType", GitAccountsStatus.GitCredentialType.NONE);
        doReturn(new ByteArrayInputStream(accountsYml.getBytes())).when(gitAccountsStatus).downloadRemoteFile();
        doReturn(mock(Path.class)).when(secretManager).decryptAsFile(anyString());
        assertTrue(gitAccountsStatus.fetchAccounts());
    }
}