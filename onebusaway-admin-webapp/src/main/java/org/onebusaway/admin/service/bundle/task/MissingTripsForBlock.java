/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.admin.service.bundle.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.onebusaway.admin.model.BundleRequestResponse;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundle;
import org.onebusaway.transit_data_federation.bundle.model.GtfsBundles;
import org.onebusaway.transit_data_federation.bundle.tasks.MultiCSVLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * 
 * This task checks for any blocks for Link in block.txt that are not
 * referenced by any trips in trips.txt.  The file "blocks_not_in_trips.csv"
 * is generated with the list of any such blocks.
 *
 */
public class MissingTripsForBlock implements  Runnable {
  private static final String FILENAME = "blocks_not_in_trips.csv";
  private static final String GTFS_BLOCK = "block.txt";
  private static final String GTFS_TRIPS = "trips.txt";
  private static final String LINK_AGENCY = "KCM";
  private static final String LINK_ROUTE = "599";
  private static Logger _log = LoggerFactory.getLogger(GtfsFullValidationTask.class);
  protected ApplicationContext _applicationContext;

  @Autowired
  public void setApplicationContext(ApplicationContext applicationContext) {
    _applicationContext = applicationContext;
  }

  @Autowired
  protected MultiCSVLogger _logger;

  public void setLogger(MultiCSVLogger logger) {
    _logger = logger;
  }

  @Override
  public void run() {
    _log.info("MissingTripsForBlock Task Starting");
    Set<String> linkBlocks = new HashSet<String>();
    Map<String,String> linkBlockEntries = new HashMap<String,String>();
    GtfsBundles gtfsBundles = getGtfsBundles(_applicationContext);
    for (GtfsBundle gtfsBundle : gtfsBundles.getBundles()) {
      String gtfsFilePath = gtfsBundle.getPath().toString();
      if (gtfsBundle.getAgencyIdMappings().containsKey(LINK_AGENCY)) {
        try (ZipFile zipFile = new ZipFile(gtfsFilePath)) {
          // Get Link block ids and block entries
          ZipEntry entry = zipFile.getEntry(GTFS_BLOCK);
          if (entry != null) {
            InputStream stream = zipFile.getInputStream(entry);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = br.readLine()) != null) {
              String[] cols = line.split(",");
              if (cols[2].equals(LINK_ROUTE)) {
                linkBlocks.add(cols[0]);
                linkBlockEntries.put(cols[0], line);
              }
            }
            br.close();
          }
          // Now check for Link blocks referenced in trips.txt
          entry = zipFile.getEntry(GTFS_TRIPS);
          if (entry != null) {
            InputStream stream = zipFile.getInputStream(entry);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = br.readLine()) != null) {
              String[] cols = line.split(",");
              String tripBlock = cols[6];
              if (linkBlocks.contains(tripBlock)) {
                linkBlocks.remove(tripBlock);
              }
            }
            br.close();
          }
        } catch (IOException e) {
          _log.error("Error reading GTFS files: " + e.getMessage());
        }
        break;
      }
    }
    // Write out results to .csv file
    _logger.header(FILENAME, "block_seq_num,block_var_num,block_route_num,block_run_num");
    for (String block : linkBlocks) {
      _logger.logCSV(FILENAME, linkBlockEntries.get(block));
    }
    _log.info("MissingTripsForBlock Task Exiting");
  }

  protected GtfsBundles getGtfsBundles(ApplicationContext context) {

    GtfsBundles bundles = (GtfsBundles) context.getBean("gtfs-bundles");
    if (bundles != null)
      return bundles;

    GtfsBundle bundle = (GtfsBundle) context.getBean("gtfs-bundle");
    if (bundle != null) {
      bundles = new GtfsBundles();
      bundles.getBundles().add(bundle);
      return bundles;
    }

    throw new IllegalStateException(
        "must define either \"gtfs-bundles\" or \"gtfs-bundle\" in config");
  }
}
