/**
*    Copyright 2011, Big Switch Networks, Inc. 
*    Originally created by David Erickson, Stanford University
* 
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package org.projectfloodlight.routing;

import org.openflow.util.HexString;

/**
 * Stores the endpoints of a route, in this case datapath ids
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class RouteId implements Cloneable, Comparable<RouteId> {
    protected Long src;
    protected Long dst;
    protected long cookie;

    public RouteId(Long src, Long dst) {
        super();
        this.src = src;
        this.dst = dst;
        this.cookie = 0;
    }

    public RouteId(Long src, Long dst, long cookie) {
        super();
        this.src = src;
        this.dst = dst;
        this.cookie = cookie;
    }

    public Long getSrc() {
        return src;
    }

    public void setSrc(Long src) {
        this.src = src;
    }

    public Long getDst() {
        return dst;
    }

    public void setDst(Long dst) {
        this.dst = dst;
    }

    public long getCookie() {
        return cookie;
    }

    public void setCookie(int cookie) {
        this.cookie = cookie;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (cookie ^ (cookie >>> 32));
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RouteId other = (RouteId) obj;
        if (cookie != other.cookie) return false;
        if (dst == null) {
            if (other.dst != null) return false;
        } else if (!dst.equals(other.dst)) return false;
        if (src == null) {
            if (other.src != null) return false;
        } else if (!src.equals(other.src)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "RouteId [src=" + HexString.toHexString(this.src) + " dst="
                + HexString.toHexString(this.dst) + "]";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int compareTo(RouteId o) {
        int result = src.compareTo(o.getSrc());
        if (result != 0)
            return result;
        return dst.compareTo(o.getDst());
    }
}
