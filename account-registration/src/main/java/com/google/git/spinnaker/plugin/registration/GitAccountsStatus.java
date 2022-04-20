/*
 * Copyright 2022 OpsMx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.git.spinnaker.plugin.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.netflix.spinnaker.clouddriver.google.config.GoogleConfigurationProperties;
import lombok.Data;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

@Data
public class GitAccountsStatus {
    private UsernamePasswordCredentialsProvider httpsUsernamePasswordCredentialsProvider;
    private UsernamePasswordCredentialsProvider httpsOAuthCredentialsProvider;
    private TransportConfigCallback sshTransportConfigCallback;
    @Value("${config.repositoryName:#{null}}")
    private String repositoryName;
    @Value("${config.filename:#{null}}")
    private String filename;
    @Value("#{'${config.credentialType}'.toUpperCase()}")
    private GitCredentialType credentialType;
    private List<GoogleConfigurationProperties.ManagedAccount> accounts;
    private GoogleConfigurationProperties credentialsConfig;

    @Autowired
    public GitAccountsStatus(@Value("${config.gitHttpsUsername:#{null}}") String gitHttpsUsername,
                             @Value("${config.gitHttpsPassword:#{null}}") String gitHttpsPassword,
                             @Value("${config.githubOAuthAccessToken:#{null}}") String githubOAuthAccessToken,
                             @Value("${config.sshPrivateKeyFilePath:#{null}}") String sshPrivateKeyFilePath,
                             @Value("${config.sshPrivateKeyPassphrase:#{null}}") String sshPrivateKeyPassphrase,
                             @Value("${config.sshKnownHostsFilePath:#{null}}") String sshKnownHostsFilePath,
                             @Value("${config.sshTrustUnknownHosts:false}") Boolean sshTrustUnknownHosts) {
        System.out.println("GitCredentialType ================================================================");
        setHttpsUsernamePasswordCredentialsProvider(gitHttpsUsername, gitHttpsPassword);
        setHttpsOAuthCredentialsProvider(githubOAuthAccessToken);
        setSshPrivateKeyTransportConfigCallback(sshPrivateKeyFilePath, sshPrivateKeyPassphrase, sshKnownHostsFilePath,
                sshTrustUnknownHosts);
    }

    @Autowired(required = false)
    public void setGoogleConfigurationProperties(GoogleConfigurationProperties credentialsConfig){
        this.credentialsConfig = credentialsConfig;
    }

    private InputStream downloadRemoteFile() {
        try {
            String REMOTE_URL = repositoryName;
            File localPath = File.createTempFile("TestGitRepository", "");
            if(!localPath.delete()) {
                throw new IOException("Could not delete temporary file " + localPath);
            }
            Repository repo;
            System.out.println("Cloning from " + REMOTE_URL + " to " + localPath);
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(REMOTE_URL)
                    .setDirectory(localPath);
            attachCredentials(cloneCommand);
            Git result = cloneCommand.call();
            System.out.println("Having repository: " + result.getRepository().getDirectory());
            repo = result.getRepository();
            repo.getObjectDatabase();
            ObjectId lastCommitId = repo.resolve(Constants.HEAD);
            RevWalk revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filename));
            if (!treeWalk.next()) {
                throw new IOException(String.format("Error reading file %s from repo %s", filename, repositoryName));
            }
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repo.open(objectId);
            InputStream inputStream = new ByteArrayInputStream(loader.getBytes());
            revWalk.dispose();
            Files.walk(localPath.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            return inputStream;
        } catch (IOException | GitAPIException e) {
            return null;
        }
    }

    private <T extends TransportCommand> void attachCredentials(T command) {
        switch (credentialType) {
            case HTTPS_USERNAME_PASSWORD:
                command.setCredentialsProvider(httpsUsernamePasswordCredentialsProvider);
                break;
            case HTTPS_GITHUB_OAUTH_TOKEN:
                command.setCredentialsProvider(httpsOAuthCredentialsProvider);
                break;
            case SSH:
                command.setTransportConfigCallback(sshTransportConfigCallback);
                break;
            case NONE:
            default:
                break;
        }
    }

    public synchronized boolean fetchAccounts() {
        System.out.println("fetchAccounts ===========================================================");
        List<GoogleConfigurationProperties.ManagedAccount> googleCredentialsDefinitions = new ArrayList<>();
        InputStream bucket = downloadRemoteFile();
        if (bucket == null) {
            return false;
        }
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(bucket);
        HashMap map = (HashMap) data.get("google");
        Boolean isEnabled = (Boolean) map.get("enabled");
        ArrayList accountsList = (ArrayList) map.get("accounts");
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Google account ====================  ");
        for(int i = 0; i < accountsList.size(); i++) {
            GoogleConfigurationProperties.ManagedAccount managedAccount = mapper.
                    convertValue(accountsList.get(i), GoogleConfigurationProperties.ManagedAccount.class);
            googleCredentialsDefinitions.add(managedAccount);
            System.out.println("Google account ====================  " + managedAccount.toString());
        }
        this.accounts = ImmutableList.copyOf(googleCredentialsDefinitions);
        return true;
    }

    public List<GoogleConfigurationProperties.ManagedAccount> getGoogleAccountsAsList() {
        return this.accounts;
    }

    private void setHttpsUsernamePasswordCredentialsProvider(String gitHttpsUsername, String gitHttpsPassword) {
        if (!StringUtils.isEmptyOrNull(gitHttpsUsername) && !StringUtils.isEmptyOrNull(gitHttpsPassword)) {
            httpsUsernamePasswordCredentialsProvider = new UsernamePasswordCredentialsProvider(gitHttpsUsername,
                    gitHttpsPassword);
        }
    }

    private void setHttpsOAuthCredentialsProvider(String githubOAuthAccessToken) {
        if (!StringUtils.isEmptyOrNull(githubOAuthAccessToken)) {
            httpsOAuthCredentialsProvider = new UsernamePasswordCredentialsProvider(githubOAuthAccessToken, "");
        }
    }

    private void setSshPrivateKeyTransportConfigCallback(String sshPrivateKeyFilePath,
                                                        String sshPrivateKeyPassphrase,
                                                        String sshKnownHostsFilePath,
                                                        boolean sshTrustUnknownHosts) {
        if (!StringUtils.isEmptyOrNull(sshPrivateKeyFilePath) && !StringUtils.isEmptyOrNull(sshPrivateKeyPassphrase)) {
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    if (sshKnownHostsFilePath == null && sshTrustUnknownHosts) {
                        session.setConfig("StrictHostKeyChecking", "no");
                    }
                }
                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch defaultJSch = super.createDefaultJSch(fs);
                    defaultJSch.addIdentity(sshPrivateKeyFilePath, sshPrivateKeyPassphrase);
                    if (sshKnownHostsFilePath != null && sshTrustUnknownHosts) {
//            log.warn("SSH known_hosts file path supplied, ignoring 'sshTrustUnknownHosts' option");
                    }
                    if (sshKnownHostsFilePath != null) {
                        defaultJSch.setKnownHosts(sshKnownHostsFilePath);
                    }
                    return defaultJSch;
                }
            };
            sshTransportConfigCallback = transport -> {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);
            };
        }
    }

    public enum GitCredentialType {
        NONE,
        HTTPS_USERNAME_PASSWORD,
        HTTPS_GITHUB_OAUTH_TOKEN,
        SSH,
    }
}
