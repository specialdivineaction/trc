package edu.tamu.tcat.trc.entries.types.bib;

import edu.tamu.tcat.trc.entries.common.DateDescription;


/**
 * Publication details for a particular work.
 */
public interface PublicationInfo
{
   /**
    * @return The place where this work was published.
    */
   String getLocation();         // TODO make first class entity

   /**
    * @return The person or organization responsible for publishing this work.
    */
   String getPublisher();        // TODO make first class entity
   
   /**
    * @return The date this work was published.
    */
   DateDescription getPublicationDate();    //  TODO should be HistoricalDate
}