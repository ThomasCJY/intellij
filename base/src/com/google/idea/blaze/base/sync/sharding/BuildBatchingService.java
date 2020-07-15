/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync.sharding;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.settings.BuildBinaryType;
import com.intellij.openapi.extensions.ExtensionPointName;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A service for splitting up a large set of targets into batches, built one at a time.
 *
 * <p>The goal is primarily to avoid OOMEs, with a secondary goal of reducing build latency.
 */
public interface BuildBatchingService {

  ExtensionPointName<BuildBatchingService> EP_NAME =
      ExtensionPointName.create("com.google.idea.blaze.BuildBatchingService");

  /**
   * Given a list of individual, un-excluded blaze targets (no wildcard target patterns), returns a
   * list of target batches.
   *
   * <p>Individual implementations may use different criteria for this batching, with the general
   * goal of avoiding OOMEs.
   *
   * <p>Returns null if batching failed.
   *
   * @param suggestedShardSize a suggestion only; may be entirely ignored by the implementation
   */
  @Nullable
  ImmutableList<ImmutableList<Label>> calculateTargetBatches(
      Set<Label> targets, BuildBinaryType buildType, int suggestedShardSize);

  /**
   * Given a list of individual, un-excluded blaze targets (no wildcard target patterns), returns a
   * list of target batches.
   *
   * <p>Iterates through all available implementations, returning the first successful result, or
   * else falling back to returning a single batch.
   */
  static ImmutableList<ImmutableList<Label>> batchTargets(
      Set<Label> targets, BuildBinaryType buildType, int suggestedShardSize) {
    return Arrays.stream(EP_NAME.getExtensions())
        .map(s -> s.calculateTargetBatches(targets, buildType, suggestedShardSize))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(ImmutableList.of(ImmutableList.copyOf(targets)));
  }
}
