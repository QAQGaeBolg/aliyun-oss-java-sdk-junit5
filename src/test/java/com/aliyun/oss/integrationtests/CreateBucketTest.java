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

import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_REGION;
import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_ALREADY_EXIST_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.INVALID_LOCATION_CONSTRAINT_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.MODIFY_STORAGE_TYPE_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.TOO_MANY_BUCKETS_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;
//import static com.aliyun.oss.model.LocationConstraint.OSS_CN_SHENZHEN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.model.*;
import org.junit.jupiter.api.*;

import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class CreateBucketTest extends TestBase {
    
    private static final int MAX_BUCKETS_ALLOWED = 10;

    @Test
    public void testPutWithDefaultLocation() {
        final String bucketName = super.bucketName + "-with-default-location";
        
        try {
        	Bucket bucket = ossClient.createBucket(bucketName);
            String loc = ossClient.getBucketLocation(bucketName);
            Assertions.assertEquals(OSS_TEST_REGION, loc);
            Assertions.assertEquals(bucket.getRequestId().length(), REQUEST_ID_LEN);
            
            // Create bucket with the same name again.
            bucket = ossClient.createBucket(bucketName);
            loc = ossClient.getBucketLocation(bucketName);
            Assertions.assertEquals(OSS_TEST_REGION, loc);
            Assertions.assertEquals(bucket.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testPutWithUnsupportedLocation() {
        final String bucketName = super.bucketName + "-with-unsupported-location";
        final String unsupportedLocation = "oss-cn-zhengzhou";
        
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.setLocationConstraint(unsupportedLocation);
        try {
            ossClient.createBucket(request);
            Assertions.fail("Create bucket should not be successful.");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.INVALID_LOCATION_CONSTRAINT, e.getErrorCode());
            Assertions.assertTrue(e.getMessage().startsWith(INVALID_LOCATION_CONSTRAINT_ERR));
        }
    }
    
    @Test
    public void testPutWithInconsistentLocation() {
        final String bucketName = super.bucketName + "-with-inconsistent-location";
        
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        // Make location constraint inconsistent with endpoint 
        request.setLocationConstraint("oss-ap-southeast-1");
        try {
            ossClient.createBucket(request);
            Assertions.fail("Create bucket should not be successful.");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.INVALID_LOCATION_CONSTRAINT, e.getErrorCode());
            Assertions.assertTrue(e.getMessage().startsWith(INVALID_LOCATION_CONSTRAINT_ERR));
        }
    }
    
    @Test
    public void testModifyExistingBucketLocation() {
        final String bucketName = super.bucketName + "-modify-existing-bucket-location";
        
        try {
            ossClient.createBucket(bucketName);
            String loc = ossClient.getBucketLocation(bucketName);
            Assertions.assertEquals(OSS_TEST_REGION, loc);
            
            // Try to modify location of existing bucket
            CreateBucketRequest request = new CreateBucketRequest(bucketName);
            request.setLocationConstraint("oss-ap-southeast-1");
            ossClient.createBucket(request);
            
            Assertions.fail("Create bucket should not be successful.");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.INVALID_LOCATION_CONSTRAINT, e.getErrorCode());
            Assertions.assertTrue(e.getMessage().startsWith(INVALID_LOCATION_CONSTRAINT_ERR));
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testPutExistingBucketWithoutOwnership() {
        final String bucketWithoutOwnership = "oss";
        
        try {
            ossClient.createBucket(bucketWithoutOwnership);    
            Assertions.fail("Create bucket should not be successful.");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.BUCKET_ALREADY_EXISTS, e.getErrorCode());
            Assertions.assertTrue(e.getMessage().startsWith(BUCKET_ALREADY_EXIST_ERR));
        }
    }
    
    @Test
    public void testInvalidBucketNames() {
        String[] invalidBucketNames = { "ab", "abcdefjhijklmnopqrstuvwxyz0123456789abcdefjhijklmnopqrstuvwxyz-a",
                "abC", "abc#", "-abc", "#abc", "-abc-", "Abcdefg", "abcdefg-" };
        
        for (String value : invalidBucketNames) {
            boolean created = false;
            try {
                ossClient.createBucket(value);
                created = true;
                Assertions.fail(String.format("Invalid bucket name %s should not be created successfully.", value));
            } catch (Exception ex) {
                Assertions.assertTrue(ex instanceof IllegalArgumentException);
            } finally {
                if (created) {
                    ossClient.deleteBucket(value);
                }
            }
        }
    }
    
    @Ignore
    public void testPutTooManyBuckets() {        
        final String bucketNamePrefix = "too-many-buckets-";
        
        try {
            List<String> existingBuckets = new ArrayList<String>();
            List<Bucket> bucketListing = ossClient.listBuckets();
            for (Bucket bkt : bucketListing) {
                existingBuckets.add(bkt.getName());
            }
            
            int remaindingAllowed = MAX_BUCKETS_ALLOWED - existingBuckets.size();            
            List<String> newlyBuckets = new ArrayList<String>();
            int i = 0;
            while (i < remaindingAllowed) {
                String bucketName = bucketNamePrefix + i;
                try {
                    ossClient.createBucket(bucketName);
                    newlyBuckets.add(bucketName);
                    i++;
                    
                    String loc = ossClient.getBucketLocation(bucketName);
                    Assertions.assertEquals(OSS_TEST_REGION, loc);
                    
                    Thread.sleep(50);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    continue;
                }
            }
            
            // Try to create (MAX_BUCKETS_ALLOWED +1)th bucket
            try {
                ossClient.createBucket(bucketNamePrefix + MAX_BUCKETS_ALLOWED);    
                Assertions.fail("Create bucket should not be successful.");
            } catch (OSSException oe) {
                Assertions.assertEquals(OSSErrorCode.TOO_MANY_BUCKETS, oe.getErrorCode());
                Assertions.assertTrue(oe.getMessage().startsWith(TOO_MANY_BUCKETS_ERR));
            } finally {
                for (String bkt : newlyBuckets) {
                    try {
                        ossClient.deleteBucket(bkt);
                    } catch (Exception e) {
                        // Ignore the exception and continue to delete remainding undesired buckets
                    }
                }
            }
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
    
    @Test
    public void testPutWithCannedACL() {
        final String bucketName = super.bucketName + "-with-canned-acl";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        try {
            // Create bucket with default(private) acl
            ossClient.createBucket(createBucketRequest);
            AccessControlList returnedAcl = ossClient.getBucketAcl(bucketName);
            Set<Grant> grants = returnedAcl.getGrants();
            Assertions.assertEquals(0, grants.size());
            Assertions.assertEquals(returnedAcl.getCannedACL(), CannedAccessControlList.Private);
            
            // Try to create existing bucket without setting acl
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(5);
            returnedAcl = ossClient.getBucketAcl(bucketName);
            grants = returnedAcl.getGrants();
            Assertions.assertEquals(0, grants.size());
            
            // Create bucket with public-read acl
            createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
            ossClient.createBucket(createBucketRequest);
            waitForCacheExpiration(5);
            returnedAcl = ossClient.getBucketAcl(bucketName);
            grants = returnedAcl.getGrants();
            Assertions.assertEquals(1, grants.size());
            Grant grant = (Grant) grants.toArray()[0];
            Assertions.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
            Assertions.assertEquals(Permission.Read, grant.getPermission());
            Assertions.assertEquals(returnedAcl.getCannedACL(), CannedAccessControlList.PublicRead);
            
            // Try to create existing bucket without setting acl
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(5);
            returnedAcl = ossClient.getBucketAcl(bucketName);
            grants = returnedAcl.getGrants();
            Assertions.assertEquals(1, grants.size());
            grant = (Grant) grants.toArray()[0];
            Assertions.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
            Assertions.assertEquals(Permission.Read, grant.getPermission());

            // Create bucket with public-read-write acl
            createBucketRequest.setCannedACL(CannedAccessControlList.PublicReadWrite);
            ossClient.createBucket(createBucketRequest);
            waitForCacheExpiration(5);
            returnedAcl = ossClient.getBucketAcl(bucketName);
            grants = returnedAcl.getGrants();
            Assertions.assertEquals(1, grants.size());
            grant = (Grant) grants.toArray()[0];
            Assertions.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
            Assertions.assertEquals(Permission.FullControl, grant.getPermission());
            Assertions.assertEquals(returnedAcl.getCannedACL(), CannedAccessControlList.PublicReadWrite);
            
            // Try to create existing bucket without setting acl
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(5);
            returnedAcl = ossClient.getBucketAcl(bucketName);
            grants = returnedAcl.getGrants();
            Assertions.assertEquals(1, grants.size());
            grant = (Grant) grants.toArray()[0];
            Assertions.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
            Assertions.assertEquals(Permission.FullControl, grant.getPermission());
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testPutWithDefaultStorageType() {
        final String bucketName = super.bucketName + "-with-default-storage-type";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        try {
            ossClient.createBucket(createBucketRequest);

            BucketList buckets = ossClient.listBuckets(bucketName, "", 100);
            Assertions.assertEquals(1, buckets.getBucketList().size());
            Assertions.assertEquals(StorageClass.Standard, buckets.getBucketList().get(0).getStorageClass());            
            Assertions.assertEquals(buckets.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Ignore
    public void testPutWithStorageType() {
        final String bucketName = super.bucketName + "-with-storage-type";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        createBucketRequest.setStorageClass(StorageClass.IA);
        try {
            ossClient.createBucket(createBucketRequest);
            AccessControlList returnedAcl = ossClient.getBucketAcl(bucketName);
            Set<Grant> grants = returnedAcl.getGrants();
            Assertions.assertEquals(0, grants.size());
            
            BucketList buckets = ossClient.listBuckets(bucketName, "", 100);
            Assertions.assertEquals(1, buckets.getBucketList().size());
            Assertions.assertEquals(StorageClass.IA, buckets.getBucketList().get(0).getStorageClass());
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Ignore
    public void testPutWithStorageTypeFunc() {
        final String bucketName = super.bucketName + "-with-storage-type-func";
        
        try {
            ossClient.createBucket(new CreateBucketRequest(bucketName).withStorageType(StorageClass.IA));
            AccessControlList returnedAcl = ossClient.getBucketAcl(bucketName);
            Set<Grant> grants = returnedAcl.getGrants();
            Assertions.assertEquals(0, grants.size());
            
            BucketList buckets = ossClient.listBuckets(bucketName, "", 100);
            Assertions.assertEquals(1, buckets.getBucketList().size());
            Assertions.assertEquals(StorageClass.IA, buckets.getBucketList().get(0).getStorageClass());
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testPutWithStorageTypeAndLocation() {
        final String bucketName = super.bucketName + "-with-storage-and-location";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        createBucketRequest.setStorageClass(StorageClass.Standard);
        createBucketRequest.setLocationConstraint(OSS_TEST_REGION);
        try {
            ossClient.createBucket(createBucketRequest);
            AccessControlList returnedAcl = ossClient.getBucketAcl(bucketName);
            Set<Grant> grants = returnedAcl.getGrants();
            Assertions.assertEquals(0, grants.size());
            System.out.println(returnedAcl.toString());
            
            BucketList buckets = ossClient.listBuckets(bucketName, "", 100);
            Assertions.assertEquals(1, buckets.getBucketList().size());
            Assertions.assertEquals(StorageClass.Standard, buckets.getBucketList().get(0).getStorageClass());
            Assertions.assertEquals(OSS_TEST_REGION, buckets.getBucketList().get(0).getLocation());
            Assertions.assertEquals(buckets.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Ignore
    public void testPutWithStorageTypeModify() {
        final String bucketName = super.bucketName + "-with-storage-type-modify";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        try {
            ossClient.createBucket(createBucketRequest);

            BucketList buckets = ossClient.listBuckets(bucketName, "", 100);
            Assertions.assertEquals(1, buckets.getBucketList().size());
            Assertions.assertEquals(StorageClass.Standard, buckets.getBucketList().get(0).getStorageClass());
           
            try {
                createBucketRequest.setStorageClass(StorageClass.IA);
                ossClient.createBucket(createBucketRequest);
                Assertions.fail("Create bucket should not be successful.");
            } catch (OSSException oe) {
                Assertions.assertEquals(OSSErrorCode.BUCKET_ALREADY_EXISTS, oe.getErrorCode());
                Assertions.assertTrue(oe.getMessage().startsWith(MODIFY_STORAGE_TYPE_ERR));
            }
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testPutWithWithStorageTypeUnsupportedLocation() {
        final String bucketName = super.bucketName + "-with-storage-unsupported-location";
        final String unsupportedLocation = "oss-cn-zhengzhou";
        
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.setStorageClass(StorageClass.Standard);
        request.setLocationConstraint(unsupportedLocation);
        try {
            ossClient.createBucket(request);
            Assertions.fail("Create bucket should not be successful.");
        } catch (OSSException e) {
            Assertions.assertEquals(OSSErrorCode.INVALID_LOCATION_CONSTRAINT, e.getErrorCode());
            Assertions.assertTrue(e.getMessage().startsWith(INVALID_LOCATION_CONSTRAINT_ERR));
        }
    }
    
    @Test
    public void testPutWithStorageTypeCompatibility() {
        final String bucketName = super.bucketName + "-with-storage-type-compatibility";
        
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        try {
            ossClient.createBucket(createBucketRequest);

            BucketList buckets = ossClient.listBuckets(bucketName, "", 100);
            Assertions.assertEquals(1, buckets.getBucketList().size());
            Assertions.assertEquals(StorageClass.Standard, buckets.getBucketList().get(0).getStorageClass());
            Assertions.assertEquals(buckets.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception ex) {
            Assertions.fail(ex.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testPutWithDataRedundancyType() {
        final String bucketName = super.bucketName + "-with-data-redundancy-type";

        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName)
                    .withDataRedundancyType(DataRedundancyType.LRS).withStorageType(StorageClass.Standard);
            ossClient.createBucket(createBucketRequest);
            Thread.sleep(2000);
            BucketInfo bucketInfo = ossClient.getBucketInfo(bucketName);
            Assertions.assertEquals(DataRedundancyType.LRS, bucketInfo.getDataRedundancyType());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }

        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName)
                    .withDataRedundancyType(DataRedundancyType.ZRS).withStorageType(StorageClass.Standard);
            ossClient.createBucket(createBucketRequest);
            Thread.sleep(2000);
            BucketInfo bucketInfo = ossClient.getBucketInfo(bucketName);
            Assertions.assertEquals(DataRedundancyType.ZRS, bucketInfo.getDataRedundancyType());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testPutWithResouceGroupId() {
        OSS client = new OSSClientBuilder().build("oss-ap-southeast-2.aliyuncs.com", TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        String bucketName = super.bucketName + "-rs-with-id";
		//repalce by correct resouce group id
        String rsId = "rg-*********";

        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName)
                    .withStorageType(StorageClass.Standard).withResourceGroupId("123");

            Assertions.assertEquals("123", createBucketRequest.getResourceGroupId());
            createBucketRequest.setResourceGroupId(rsId);
            Assertions.assertEquals(rsId, createBucketRequest.getResourceGroupId());

            client.createBucket(createBucketRequest);
            Thread.sleep(2000);
            BucketInfo bucketInfo = client.getBucketInfo(bucketName);
            Assertions.assertEquals(rsId, bucketInfo.getBucket().getResourceGroupId());

            ListBucketsRequest listBucketsRequest = new ListBucketsRequest(bucketName, "", 1);
            Assertions.assertEquals(null, listBucketsRequest.getResourceGroupId());

            BucketList list = client.listBuckets(listBucketsRequest);
            Assertions.assertEquals(null, list.getBucketList().get(0).getResourceGroupId());

            listBucketsRequest.setResourceGroupId(rsId);
            Assertions.assertEquals(rsId, listBucketsRequest.getResourceGroupId());
            list = client.listBuckets(listBucketsRequest);
            Assertions.assertEquals(rsId, list.getBucketList().get(0).getResourceGroupId());

            GetBucketResourceGroupResult result = client.getBucketResourceGroup(bucketName);
            Assertions.assertEquals(rsId, result.getResourceGroupId());

        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        } finally {
            client.deleteBucket(bucketName);
        }

        bucketName = super.bucketName + "-rs-without-id";

        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName)
                    .withStorageType(StorageClass.Standard);
            Assertions.assertEquals(null, createBucketRequest.getResourceGroupId());

            client.createBucket(createBucketRequest);
            Thread.sleep(2000);
            BucketInfo bucketInfo = client.getBucketInfo(bucketName);
            Assertions.assertNotNull(bucketInfo.getBucket().getResourceGroupId());
            String defaultRsId = bucketInfo.getBucket().getResourceGroupId();

            ListBucketsRequest listBucketsRequest = new ListBucketsRequest(bucketName, "", 1)
                    .withResourceGroupId(defaultRsId);
            Assertions.assertEquals(defaultRsId, listBucketsRequest.getResourceGroupId());

            BucketList list = client.listBuckets(listBucketsRequest);
            Assertions.assertEquals(defaultRsId, list.getBucketList().get(0).getResourceGroupId());

            /*
            listBucketsRequest = new ListBucketsRequest(bucketName, "", 1)
                    .withResourceGroupId("");
            list = client.listBuckets(listBucketsRequest);
            Assertions.assertEquals(defaultRsId, list.getBucketList().get(0).getResourceGroupId());
            */

            GetBucketResourceGroupResult result = client.getBucketResourceGroup(bucketName);
            Assertions.assertEquals(defaultRsId, result.getResourceGroupId());

            SetBucketResourceGroupRequest setBucketResourceGroupRequest = new SetBucketResourceGroupRequest(bucketName, rsId);
            client.setBucketResourceGroup(setBucketResourceGroupRequest);

            bucketInfo = client.getBucketInfo(bucketName);
            Assertions.assertEquals(rsId, bucketInfo.getBucket().getResourceGroupId());

        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        } finally {
            client.deleteBucket(bucketName);
        }

        //invalid rs id
        bucketName = super.bucketName + "-rs-with-invalid-id";

        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName)
                    .withStorageType(StorageClass.Standard).withResourceGroupId("invalid-rs-id");
            client.createBucket(createBucketRequest);
            Assertions.fail("shuold not here");
        } catch (OSSException e) {
            Assertions.assertEquals("InvalidArgument", e.getErrorCode());
        } catch (Exception ex) {
            Assertions.fail("shuold not here");
        }
    }
}
