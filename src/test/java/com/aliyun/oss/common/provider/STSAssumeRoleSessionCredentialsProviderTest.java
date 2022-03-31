/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.provider;

import java.io.ByteArrayInputStream;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.BasicCredentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import org.junit.jupiter.api.*;
import org.junit.Test;

public class STSAssumeRoleSessionCredentialsProviderTest extends TestBase {

    @Test
    public void testStsAssumeRoleCredentialsProvider() {
        try {
            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID, TestConfig.USER_ACCESS_KEY_ID,
                            TestConfig.USER_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                    .withExpiredDuration(900);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assertions.assertTrue(credentials.useSecurityToken());
            Assertions.assertFalse(credentials.willSoonExpire());
            Assertions.assertTrue(credentials.getAccessKeyId().startsWith("STS."));
            Assertions.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assertions.assertTrue(credentials.getSecretAccessKey().length() > 0);
            Assertions.assertTrue(credentials.getSecurityToken().length() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testStsAssumeRoleCredentialsProviderExpire() {
        try {
            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID, TestConfig.USER_ACCESS_KEY_ID,
                            TestConfig.USER_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                    .withExpiredFactor(0.001).withExpiredDuration(2000);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assertions.assertFalse(credentials.willSoonExpire());

            Thread.sleep(3000);

            Assertions.assertTrue(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testStsAssumeRoleCredentialsProviderRefresh() {
        try {
            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID, TestConfig.USER_ACCESS_KEY_ID,
                            TestConfig.USER_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                    .withExpiredFactor(0.001).withExpiredDuration(2000);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assertions.assertFalse(credentials.willSoonExpire());
            Thread.sleep(3000);
            Assertions.assertTrue(credentials.willSoonExpire());

            BasicCredentials freshCredentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assertions.assertFalse(freshCredentials.willSoonExpire());
            Assertions.assertFalse(freshCredentials.getAccessKeyId().equals(credentials.getAccessKeyId()));
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testStsAssumeRoleCredentialsProviderNegative() {
        try {
            CredentialsProviderFactory.newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID,
                    TestConfig.USER_ACCESS_KEY_ID, TestConfig.USER_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                    .withExpiredDuration(899);
            Assertions.fail("RamUtils.newStsAssumeRoleCredentialsProvider should not be successful.");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            CredentialsProviderFactory.newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID,
                    TestConfig.USER_ACCESS_KEY_ID, TestConfig.USER_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                    .withExpiredDuration(3601);
            Assertions.fail("RamUtils.newStsAssumeRoleCredentialsProvider should not be successful.");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            CredentialsProviderFactory.newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID,
                    TestConfig.USER_ACCESS_KEY_ID, TestConfig.USER_ACCESS_KEY_SECRET, "").withExpiredDuration(3601);
            Assertions.fail("RamUtils.newStsAssumeRoleCredentialsProvider should not be successful.");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof IllegalArgumentException);
        }

    }

    @Test
    public void testStsAssumeRoleCredentialsProviderInOss() {
        try {
            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION_ID, TestConfig.USER_ACCESS_KEY_ID,
                            TestConfig.USER_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                    .withExpiredDuration(900);

            String key = "test.txt";
            String content = "HelloOSS";
            String bucketName = getRandomBucketName();

            OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_ENDPOINT, credentialsProvider);
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(2);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.deleteObject(bucketName, key);
            ossClient.deleteBucket(bucketName);
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }
}
