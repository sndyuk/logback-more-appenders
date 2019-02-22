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
