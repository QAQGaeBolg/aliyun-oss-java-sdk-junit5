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

package com.aliyun.oss.integrationtests;

import org.junit.jupiter.api.*;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.SetBucketStorageCapacityRequest;
import com.aliyun.oss.model.UserQos;

public class BucketUserQosTest extends TestBase {

    @Test
    public void testDefaultBucketStorageCapacity() {
        try {
            UserQos userQos = new UserQos();
            
            userQos = ossClient.getBucketStorageCapacity(bucketName);
            Assertions.assertEquals(userQos.getStorageCapacity(), -1);
            Assertions.assertEquals(userQos.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }
    
    @Test
    public void testSetBucketStorageCapacity() {
        try {
            UserQos userQos = new UserQos(-1);
            ossClient.setBucketStorageCapacity(bucketName, userQos);
            
            userQos = ossClient.getBucketStorageCapacity(bucketName);
            Assertions.assertEquals(userQos.getStorageCapacity(), -1);
            Assertions.assertEquals(userQos.getRequestId().length(), REQUEST_ID_LEN);
            
            userQos.setStorageCapacity(10000);
            ossClient.setBucketStorageCapacity(new SetBucketStorageCapacityRequest(bucketName).withUserQos(userQos));
            
            userQos = ossClient.getBucketStorageCapacity(bucketName);
            Assertions.assertEquals(userQos.getStorageCapacity(), 10000);
            Assertions.assertEquals(userQos.getRequestId().length(), REQUEST_ID_LEN);

        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
    
    @Test
    public void testSetBucketStorageCapacityNegative() {
        
        try {
            UserQos userQos = new UserQos(-2);
            ossClient.setBucketStorageCapacity(bucketName, userQos);
            Assertions.fail("Set bucket storage capacity should not be successful");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        try {
            UserQos userQos = new UserQos(-3);
            ossClient.setBucketStorageCapacity(new SetBucketStorageCapacityRequest(bucketName).withUserQos(userQos));
            Assertions.fail("Set bucket storage capacity should not be successful");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
    }
    
}
