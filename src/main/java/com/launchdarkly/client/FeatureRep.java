package com.launchdarkly.client;


import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

class FeatureRep<E> {
  String key;
  String salt;
  boolean on;
  List<Variation<E>> variations;

  private static final float long_scale = (float)0xFFFFFFFFFFFFFFFL;

  public FeatureRep() {

  }

  private Float paramForUser(LDUser user) {
    String idHash;
    String hash;

    if (user.getKey() != null) {
      idHash = user.getKey();
    }
    else {
      return null;
    }

    hash = DigestUtils.sha1Hex(key + "." + salt + "." + idHash).substring(0,15);

    long longVal = Long.parseLong(hash, 16);

    Float result =  Float.valueOf((float) longVal / long_scale);

    return result;
  }

  public E evaluate(LDUser user) {
    Float param = paramForUser(user);

    if (param == null) {
      return null;
    }
    else {
      for (Variation<E> variation : variations) {
        if (variation.matchSegment(user)) {
          return variation.value;
        }
      }

      float sum = 0.0f;
      for (Variation<E> variation : variations) {
        sum += ((float)variation.weight) / 100.0;

        if (param < sum) {
          return variation.value;
        }

      }
    }
      return null;
  }
}
