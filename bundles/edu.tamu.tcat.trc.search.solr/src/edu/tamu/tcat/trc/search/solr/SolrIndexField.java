/*
 * Copyright 2015 Texas A&M Engineering Experiment Station
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
package edu.tamu.tcat.trc.search.solr;

public interface SolrIndexField<FieldType>
{
   /**
    * @return The name of this field.
    */
   String getName();

   /**
    * @return The Java type of values associated with this field.
    */
   Class<FieldType> getType();

   /**
    * Convert to a string suitable to be used as a SolrJ REST query parameter value.
    *
    * @param value
    * @return
    * @throws SearchException
    */
   String toSolrValue(FieldType value) throws SearchException;
}
