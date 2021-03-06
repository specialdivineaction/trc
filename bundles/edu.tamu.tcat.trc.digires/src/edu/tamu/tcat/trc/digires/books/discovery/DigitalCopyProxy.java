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
package edu.tamu.tcat.trc.digires.books.discovery;

import edu.tamu.tcat.trc.digires.books.resolve.CopyResolverStrategy;
import edu.tamu.tcat.trc.digires.books.resolve.DigitalCopy;

/**
 *  Defines simple metadata for uniquely identifying a digital copy and presenting basic
 *  information about that copy to aid in the discovery process. Note that this is maintained
 *  separately from {@link DigitalCopy} since the full copy may require additional resources to
 *  retrieve (e.g., a remote REST request) and will be handled by a client that is tightly
 *  coupled to the {@code DigitalCopy} implementation. {@code DigitalCopyProxy} instances are
 *  intended to be accessed via their interface.
 */
public interface DigitalCopyProxy
{
   /**
    * @return An identifier that uniquely identifies this copy within the scope of a particular
    *       {@link CopyResolverStrategy}. Note that this identifier should be in a format that
    *       can be recognized and parsed only by the the {@link CopyResolverStrategy} that
    *       created this proxy.
    */
   String getIdentifier();

   /**
    * @return A short title for this copy suitable for display. This will be used to assist
    *       end users in assessing the relevance of this proxy for their particular needs.
    */
   String getTitle();

   /**
    * @return A short description of the copy.
    */
   String getDescription();

   /**
    * @return The service that hosts and/or provides access to the digital copy of this work,
    *       such as Google Books, HathiTrust, Internet Archive of Local. Suitable for display.
    */
   String getCopyProvider();

   /**
    * @return Basic information about the source of the digital copy such as University of
    *       Michigan. Intended for user display to aid discovery of relevant copies rather that
    *       to provide detailed metadata about the location of the source object or the
    *       digitization process.
    */
   String getSourceSummary();

   /**
    * @return A string based representation of the publication date.
    */
   String getPublicationDate();

   /**
    * @return The rights to access a digital content, defined by the service provider.
    */
   String getRights();
}
