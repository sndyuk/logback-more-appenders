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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesCredentials;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public abstract class AwsAppender<E> extends UnsynchronizedAppenderBase<E> {

    protected AwsConfig config;
    protected AWSCredentials credentials;

    @Override
    public void start() {
        try {
            super.start();
            if (config.getCredentialFilePath() != null
                    && config.getCredentialFilePath().length() > 0) {
                this.credentials = new PropertiesCredentials(getClass().getClassLoader()
                        .getResourceAsStream(config.getCredentialFilePath()));
            } else {
                this.credentials =
                        DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
            }
        } catch (Exception e) {
            addWarn("Could not initialize " + AwsAppender.class.getCanonicalName()
                    + " ( will try to initialize again later ): " + e);
        }
    }

    public static class AwsConfig {
        private String credentialFilePath;
        private String region;

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
    }
}
