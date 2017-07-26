/*
 * Minio Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2016 Minio, Inc.
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

package io.minio.policy;

import java.util.Hashtable;

class ConditionMap extends Hashtable<String, ConditionKeyMap> {
  public ConditionMap() {
    super();
  }


  public ConditionMap(String key, ConditionKeyMap value) {
    super();

    this.put(key, value);
  }


  @Override
  public ConditionKeyMap put(String key, ConditionKeyMap value) {
    ConditionKeyMap existingValue = super.get(key);

    if (existingValue == null) {
      existingValue = new ConditionKeyMap(value);
    } else {
      existingValue.putAll(value);
    }

    return super.put(key, existingValue);
  }
}
