package com.launchdarkly.client;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class Variation<E> {
  E value;
  int weight;
  List<TargetRule> targets;
  private final Logger logger = LoggerFactory.getLogger(Variation.class);

  public Variation() {

  }

  Variation(Builder<E> b) {
    this.value = b.value;
    this.weight = b.weight;
    this.targets = new ArrayList<TargetRule>(b.targets);
  }

  public boolean matchTarget(LDUser user) {
    for (TargetRule target: targets) {
      if (target.matchTarget(user)) {
        return true;
      }
    }
    return false;
  }

  static class Builder<E> {
    E value;
    int weight;
    List<TargetRule> targets;

    Builder(E value, int weight) {
      this.value = value;
      this.weight = weight;
      targets = new ArrayList<TargetRule>();
    }

    Builder<E> target(TargetRule rule) {
      targets.add(rule);
      return this;
    }

    Variation<E> build() {
      return new Variation<E>(this);
    }

  }

  static class TargetRule {
    String attribute;
    String operator;
    List<JsonElement> values;

    private final Logger logger = LoggerFactory.getLogger(TargetRule.class);

    TargetRule(String attribute, String operator, List<JsonElement> values) {
      this.attribute = attribute;
      this.operator = operator;
      this.values = new ArrayList<JsonElement>(values);
    }

    TargetRule(String attribute, List<JsonElement> values) {
      this(attribute, "in", values);
    }

    public boolean matchTarget(LDUser user) {
      JsonElement uValue = null;
      if (attribute.equals("key")) {
        if (user.getKey() != null) {
          uValue = new JsonPrimitive(user.getKey());
        }
      }
      else if (attribute.equals("ip") && user.getIp() != null) {
        if (user.getIp() != null) {
          uValue = new JsonPrimitive(user.getIp());
        }
      }
      else if (attribute.equals("country")) {
        if (user.getCountry() != null) {
          uValue = new JsonPrimitive(user.getCountry().getAlpha2());
        }
      }
      else { // Custom attribute
        JsonElement custom = user.getCustom(attribute);

        if (custom != null) {
          if (custom.isJsonArray()) {
            JsonArray array = custom.getAsJsonArray();
            for (JsonElement elt: array) {
              if (! elt.isJsonPrimitive()) {
                logger.error("Invalid custom attribute value in user object: " + elt);
                return false;
              }
              else if (values.contains(elt)) {
                return true;
              }
            }
            return false;
          }
          else if (custom.isJsonPrimitive()) {
            return values.contains(custom);
          }
        }
        return false;
      }
      if (uValue == null) {
        return false;
      }
      return values.contains(uValue);
    }
  }
}
