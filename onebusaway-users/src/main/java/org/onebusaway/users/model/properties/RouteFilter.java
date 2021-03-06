/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.users.model.properties;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.onebusaway.users.client.model.RouteFilterBean;

/**
 * A route filter, as filtered by a set of route ids. If the set of ids is
 * empty, we consider all routes to be enabled. if the set of ids is not empty,
 * then we consider only routes with ids contained in the filter set to be
 * enabled.
 * 
 * @author bdferris
 * @see RouteFilterBean
 */
public final class RouteFilter implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Set<String> routeIds;

  public RouteFilter() {
    this(new HashSet<String>());
  }

  public RouteFilter(Set<String> routeIds) {
    this.routeIds = new HashSet<String>(routeIds);
  }

  public Set<String> getRouteIds() {
    return routeIds;
  }
}
