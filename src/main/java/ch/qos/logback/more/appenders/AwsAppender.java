/**
 * Copyright (c) 2018 sndyuk <sanada@sndyuk.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package ch.qos.logback.more.appenders;

import ch.qos.logback.core.AppenderBase;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;


public abstract class AwsAppender<E> extends AppenderBase<E> {

    protected AwsConfig config;
    protected AWSCredentialsProvider credentialsProvider;
    protected AWSCredentials credentials;

    @Override
    public void start() {
        try {
            if (config.getCredentialFilePath() != null
              && config.getCredentialFilePath().length() > 0) {
                this.credentials = new PropertiesCredentials(getClass().getClassLoader()
                  .getResourceAsStream(config.getCredentialFilePath()));
            } else if (config.getProfile() != null && config.getProfile().length() > 0) {
                this.credentialsProvider = new ProfileCredentialsProvider(config.getProfile());
            } else {
                this.credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
            }
            super.start();
        } catch (Exception e) {
            addWarn("Could not initialize " + AwsAppender.class.getCanonicalName()
                    + " ( will try to initialize again later ): " + e);
        }
    }

    public static class AwsConfig {
        private String credentialFilePath;
        private String region;
        private String profile;

        public void setCredentialFilePath(String credentialFilePath) {
            this.credentialFilePath = credentialFilePath;
        }

        public String getCredentialFilePath() {
            return credentialFilePath;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }
    }
}
