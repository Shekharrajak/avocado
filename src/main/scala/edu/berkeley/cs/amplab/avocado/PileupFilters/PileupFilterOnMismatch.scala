/*
 * Copyright (c) 2013. Regents of the University of California
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

package edu.berkeley.cs.amplab.avocado.filters.pileup

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import edu.berkeley.cs.amplab.adam.avro.ADAMPileup

/**
 * Class implementing a filter that only returns Pileups that contain
 * a mismatch on any bases in the pileup.
 */
class PileupFilterOnMismatch extends PileupFilter {

  val filterName = "FilterOnMismatchSNP"

  /**
   * Filter to only return pileups with mismatches.
   *
   * @param[in] pileups An RDD containing reference oriented stacks of nucleotides.
   * @return An RDD containing only pileups that contain at least one mismatch.
   */
  override def filter (pileups: RDD [ADAMPileup]): RDD [ADAMPileup] = {

    val pileupsGroupedByPosition = pileups.groupBy ((pileup: ADAMPileup) => pileup.getPosition.toLong, reducePartitions)
    val positionCount = pileupsGroupedByPosition.count

    log.info (positionCount.toString + " rods to filter.")
      
    val multipleEvidence = pileupsGroupedByPosition.filter ((kv: (Long, Seq[ADAMPileup])) => kv._2.length > 1)
      .map (kv => (kv._1, kv._2.toList))
      .flatMap ((kv: (Long, List[ADAMPileup])) => kv._2)

    log.info (multipleEvidence.count.toString + " rods with evidence of multiple alleles.")

    val singleEvidenceMismatch = pileupsGroupedByPosition.filter ((kv: (Long, Seq[ADAMPileup])) => kv._2.length == 1)
      .map (kv => (kv._1, kv._2.toList))
      .flatMap ((kv: (Long, List[ADAMPileup])) => kv._2)
      .filter ((pileup: ADAMPileup) => pileup.getReadBase != pileup.getReferenceBase)

    log.info (singleEvidenceMismatch.toString + " rods with evidence of a single non-reference allele.")

    multipleEvidence ++ singleEvidenceMismatch
  }
}
