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

import com.netflix.spinnaker.clouddriver.google.security.GoogleNamedAccountCredentials;
import com.netflix.spinnaker.credentials.CredentialsLifecycleHandler;
import com.netflix.spinnaker.credentials.MapBackedCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class GoogleCredentialsRepository extends MapBackedCredentialsRepository<GoogleNamedAccountCredentials> {

    public GoogleCredentialsRepository(CredentialsLifecycleHandler<GoogleNamedAccountCredentials> eventHandler) {
        super("gce", eventHandler);
        log.info("{} started *************************************", this.getClass().getSimpleName());
    }

    public GoogleCredentialsRepository(String type, @Nullable CredentialsLifecycleHandler<GoogleNamedAccountCredentials> eventHandler) {
        super("gce", eventHandler);
    }
}
