/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.logistics.plugin.policy;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.policy.Policy;
import org.cougaar.core.util.UniqueObject;
import java.util.HashSet;

/**
 * class to hold instance of Policy using Relay to propagate itself to a given role
 */
public class LogisticsPolicy extends java.lang.Object
implements Relay.Source, Relay.Target, UniqueObject {

	private MessageAddress source;
	private String authority;
	private HashSet _targets = new HashSet();
	private Policy policy;
	private UID uid;
	private String role;
/**
 * constructor
 * @param policy Policy for LogisticsPolicy to hold
 * @param source MessageAddress of Organization which is sending the LogisticsPolicy
 * @param target Set of MessageAddresses which the LogisticsPolicy to be delivered
 * @param authority name of the originator of this LogisticsPolicy
 * @param role Role to which the Policy should be propagated
 */
	public LogisticsPolicy(Policy policy, MessageAddress source, Set target, String authority, String role) {
		this.policy = policy;
		this.source = source;
		this.authority = authority;
		this._targets.addAll(target);
		this.role = role;
	}

/**
 * constructor used by Factory
 * @param other LogisticsPolicy which prompted the creation of this new one
 * @param src MessageAddress of sender of the LogisticsPolicy
 */

	protected LogisticsPolicy(LogisticsPolicy other, MessageAddress src) {
		this(other.getPolicy(),
	 	src,
	 	Collections.EMPTY_SET,
		other.getAuthority(),
		other.getRole());
		this.setUID(other.getUID());
	}
/**
 * method to retrieve role to which this LogisticsPolicy is to be propagated
 */

	public String getRole() {
		return role;
	}

/**
 * method to retrieve originator
 */
	public String getAuthority() {
		return authority;
	}

/**
 * method to retrieve the sender
 */
	public MessageAddress getSource(){
		return source;
	}

/**
 * method to set sender
 * @param ma MessageAddress of sender
 */
	public void setSource(MessageAddress ma) {
		this.source = ma;
	}

/**
 * returns null
 */
	public Object getResponse() {
		return null;
	}

/**
 * returns Relay.NO_CHANGE
 */
	public int updateResponse(MessageAddress t, Object response) {
		return Relay.NO_CHANGE;
	}

/**
 * returns Relay.NO_CHANGE
 */
	public int updateContent(Object content, Token token) {
		return NO_CHANGE;
	}

/**
 * returns this
 */
	public Object getContent() {
		return this;
	}

/**
 * method to get Set of MessageAddresses of destinations for this LogisticsPolicy
 */
	public Set getTargets() {
		if (_targets != null) {
			return _targets;
		} else {
			return Collections.EMPTY_SET;
		}
	}

//	public void addTargets(Set newTargets) {
//		if (_targets.isEmpty()) {
//			_targets = newTargets;
//		} else {
//			this._targets.addAll(newTargets);
//		}
//		if (_targets != null){
//			d.print("addTargets - all targets: " + this._targets.toString());
//		} else d.print("addTargets - targets are null");
//	}
	
/**
 * method to set targets
 * @param newTargets Set of MessageAddresses to direct this LogisticsPolicy
 */
	public void setTargets(Set newTargets) {
		this._targets = new HashSet(newTargets);
	}

	public void addTarget(MessageAddress target) {
		if (_targets == null) {
			_targets = new HashSet();
		}
		if (_targets instanceof HashSet) {
		}
		_targets.add(target);
	}

/**
 * method to get UID
 */
	public UID getUID(){
		return uid;
	}

/**
 * method to set UID
 * @param uid new UID for the LogisticsPolicy
 */
	public void setUID(UID uid){
		this.uid = uid;
	}

/**
 * method to get Policy from within the LogisticsPolicy
 */
	public Policy getPolicy(){
		return policy;
	}

/**
 * Factory to create new LogisticsPolicies
 */
	private static final class SimpleRelayFactory
	implements TargetFactory, java.io.Serializable {

		public static final SimpleRelayFactory INSTANCE = 
		new SimpleRelayFactory();

		private SimpleRelayFactory() {}

/**
 * method to create new instance of LogisticsPolicy
 * @param uid UID
 * @param source MessageAddress of sender
 * @param content
 * @param token
 */
		public Relay.Target create(
			UID uid, 
			MessageAddress source, 
			Object content,
			Token token) {
			LogisticsPolicy p = null;
			try {
				p = (LogisticsPolicy) content;
			} catch (ClassCastException cce) {
				;
			}
			return new LogisticsPolicy(
			p, source);
		}

		private Object readResolve() {
			return INSTANCE;
		}
	};
	
/**
 * method to retrieve instance of SimpleRelayFactory
 */
	public TargetFactory getTargetFactory() {
		return SimpleRelayFactory.INSTANCE;
	
	}	
}
