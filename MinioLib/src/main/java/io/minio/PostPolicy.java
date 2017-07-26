/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 Minio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import io.minio.errors.InvalidArgumentException;


/**
 * Post policy information to be used to generate presigned post policy form-data.
 */
public class PostPolicy {
  private static final String ALGORITHM = "AWS4-HMAC-SHA256";

  private final String bucketName;
  private final String objectName;
  private final boolean startsWith;
  private final DateTime expirationDate;
  private String contentType;
  private String contentEncoding;
  private long contentRangeStart;
  private long contentRangeEnd;


  public PostPolicy(String bucketName, String objectName, DateTime expirationDate)
    throws InvalidArgumentException {
    this(bucketName, objectName, false, expirationDate);
  }


  /**
   * Creates PostPolicy for given bucket name, object name, string to match object name starting with
   * and expiration time.
   */
  public PostPolicy(String bucketName, String objectName, boolean startsWith, DateTime expirationDate)
    throws InvalidArgumentException {
    if (bucketName == null) {
      throw new InvalidArgumentException("null bucket name");
    }
    this.bucketName = bucketName;

    if (objectName == null) {
      throw new InvalidArgumentException("null object name or prefix");
    }
    this.objectName = objectName;

    this.startsWith = startsWith;

    if (expirationDate == null) {
      throw new InvalidArgumentException("null expiration date");
    }
    this.expirationDate = expirationDate;
  }


  /**
   * Sets content type.
   */
  public void setContentType(String contentType) throws InvalidArgumentException {
    if (Strings.isNullOrEmpty(contentType)) {
      throw new InvalidArgumentException("empty content type");
    }

    this.contentType = contentType;
  }


  /**
   * Sets content encoding.
   */
  public void setContentEncoding(String contentEncoding) throws InvalidArgumentException {
    if (Strings.isNullOrEmpty(contentEncoding)) {
      throw new InvalidArgumentException("empty content encoding");
    }

    this.contentEncoding = contentEncoding;
  }


  /**
   * Sets content length.
   */
  public void setContentLength(long contentLength) throws InvalidArgumentException {
    if (contentLength <= 0) {
      throw new InvalidArgumentException("negative content length");
    }

    this.setContentRange(contentLength, contentLength);
  }


  /**
   * Sets content range.
   */
  public void setContentRange(long startRange, long endRange) throws InvalidArgumentException {
    if (startRange <= 0 || endRange <= 0) {
      throw new InvalidArgumentException("negative start/end range");
    }

    if (startRange > endRange) {
      throw new InvalidArgumentException("start range is higher than end range");
    }

    this.contentRangeStart = startRange;
    this.contentRangeEnd = endRange;
  }


  /**
   * Returns bucket name.
   */
  public String bucketName() {
    return this.bucketName;
  }


  private byte[] marshalJson(ArrayList<String[]> conditions) throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder();
    Joiner joiner = Joiner.on("\",\"");

    sb.append("{");

    if (expirationDate != null) {
      sb.append("\"expiration\":" + "\"" + expirationDate.toString(DateFormat.EXPIRATION_DATE_FORMAT) + "\"");
    }

    if (!conditions.isEmpty()) {
      sb.append(",\"conditions\":[");

      ListIterator<String[]> iterator = conditions.listIterator();
      while (iterator.hasNext()) {
        sb.append("[\"" + joiner.join(iterator.next()) + "\"]");
        if (iterator.hasNext()) {
          sb.append(",");
        }
      }

      sb.append("]");
    }

    sb.append("}");

    return sb.toString().getBytes("UTF-8");
  }

  /**
   * Returns form data of this post policy.
   * @deprecated Use {@link #formData(String accessKey, String secretKey, String region)} instead.
   */
  @Deprecated
  public Map<String,String> formData(String accessKey, String secretKey)
    throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
    return makeFormData(accessKey, secretKey, BucketRegionCache.INSTANCE.region(this.bucketName));
  }

  /**
   * Returns form data of this post policy setting the provided region.
   */
  public Map<String,String> formData(String accessKey, String secretKey, String region)
      throws NoSuchAlgorithmException, InvalidKeyException, InvalidArgumentException, UnsupportedEncodingException {

    if (Strings.isNullOrEmpty(region)) {
      throw new InvalidArgumentException("empty region");
    }

    return makeFormData(accessKey, secretKey, region);
  }

  protected Map<String,String> makeFormData(String accessKey, String secretKey, String region)
      throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {

    ArrayList<String[]> conditions = new ArrayList<>();
    Map<String, String> formData = new HashMap<>();

    conditions.add(new String[]{"eq", "$bucket", this.bucketName});
    formData.put("bucket", this.bucketName);

    if (this.startsWith) {
      conditions.add(new String[]{"starts-with", "$key", this.objectName});
      formData.put("key", this.objectName);
    } else {
      conditions.add(new String[]{"eq", "$key", this.objectName});
      formData.put("key", this.objectName);
    }

    if (this.contentType != null) {
      conditions.add(new String[]{"eq", "$Content-Type", this.contentType});
      formData.put("Content-Type", this.contentType);
    }

    if (this.contentEncoding != null) {
      conditions.add(new String[]{"eq", "$Content-Encoding", this.contentEncoding});
      formData.put("Content-Encoding", this.contentEncoding);
    }

    if (this.contentRangeStart > 0 && this.contentRangeEnd > 0) {
      conditions.add(new String[]{"content-length-range", Long.toString(this.contentRangeStart),
                                  Long.toString(this.contentRangeEnd)});
    }

    conditions.add(new String[]{"eq", "$x-amz-algorithm", ALGORITHM});
    formData.put("x-amz-algorithm", ALGORITHM);

    DateTime date = new DateTime();
    String credential = Signer.credential(accessKey, date, region);
    conditions.add(new String[]{"eq", "$x-amz-credential", credential});
    formData.put("x-amz-credential", credential);

    String amzDate = date.toString(DateFormat.AMZ_DATE_FORMAT);
    conditions.add(new String[]{"eq","$x-amz-date", amzDate});
    formData.put("x-amz-date", amzDate);

    String policybase64 = BaseEncoding.base64().encode(this.marshalJson(conditions));
    String signature =  Signer.postPresignV4(policybase64, secretKey, date, region);

    formData.put("policy", policybase64);
    formData.put("x-amz-signature", signature);

    return formData;
  }
}
