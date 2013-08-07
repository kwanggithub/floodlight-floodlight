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

package org.projectfloodlight.core.internal;

/**
 * This exception indicates an error or unexpected message during
 * message handling. E.g., if an OFMessage is received that is illegal or
 * unexpected given the current handshake state.
 *
 * We don't allow wrapping other exception in a switch state exception. We
 * only log the SwitchStateExceptions message so the casuing exceptions
 * stack trace is generally not available.
 *
 */
public class SwitchStateException extends IllegalArgumentException {

    private static final long serialVersionUID = 9153954512470002631L;

    public SwitchStateException() {
        super();
    }

    public SwitchStateException(String arg0) {
        super(arg0);
    }

    public SwitchStateException(Throwable arg0) {
        super(arg0);
    }

}