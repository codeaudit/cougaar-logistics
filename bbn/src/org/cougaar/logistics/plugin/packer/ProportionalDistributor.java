/*
 * <copyright>
 *  
 *  Copyright 1999-2004 Honeywell Inc
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

package org.cougaar.logistics.plugin.packer;

import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.plan.*;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class is similar to the DefaultDistributor, but allocates quantities
 * proportionally, based on input task quantities, rather than just
 * evenly.  Code cribbed extensively from the definition of DefaultDistributor
 * @see org.cougaar.planning.ldm.plan.AllocationResultDistributor.DefaultDistributor
 */
public class ProportionalDistributor implements AllocationResultDistributor, Serializable {
  private transient LoggingService myLoggingService = null;
  public static ProportionalDistributor DEFAULT_PROPORTIONAL_DISTRIBUTOR =
      new ProportionalDistributor();

  public ProportionalDistributor() {
  }

  public void setLoggingService(LoggingService ls) {
    myLoggingService = ls;
  }

  public LoggingService getLoggingService() {
    return myLoggingService;
  }

  public TaskScoreTable calculate(Vector parents, AllocationResult ar) {
    int l = parents.size();

    if (l == 0 || ar == null) return null;

    if (!ar.isDefined(AspectType.QUANTITY)) {
      // if there's no quantity in the Allocation result, then we
      // can just use the Default Distributor
      return AllocationResultDistributor.DEFAULT.calculate(parents, ar);
    } else {
      // this variable is never used, should we delete it?
      double quantAchieved = ar.getValue(AspectType.QUANTITY);
      AllocationResult results[] = new AllocationResult[l];
      double quantProportions[] = new double[l];
      // the following block serves to set the quantProportions array
      {
        // these variables are lexically scoped here --- just used
        // to compute proportional share...
        double totalRequestedQuant = 0.0;
        double quantsRequested[] = new double[l];
        for (int i = 0; i < l; i++) {
          double thisQuant = getTaskQuantity(((Task) parents.get(i)));
          if (thisQuant == -1.0) {
            // no quantity was requested, set to zero

            if (getLoggingService() == null) {
              System.err.println("ProportionalDistributor: attempting to allocate a proportional share of quantity to a Task which requests no quantity.");
            } else {
              getLoggingService().warn("ProportionalDistributor: attempting to allocate a proportional share of quantity to a Task which requests no quantity.");
            }
            thisQuant = 0.0;
          }
          totalRequestedQuant += thisQuant;
          quantsRequested[i] = thisQuant;
        }
        if (totalRequestedQuant == 0.0) {
          // make sure we catch the boundary condition!
          for (int i = 0; i < l; i++) {
            quantProportions[i] = 0.0;
          }
        } else {
          for (int i = 0; i < l; i++) {
            quantProportions[i] = quantsRequested[i] / totalRequestedQuant;
          }
        }
      }

      // build a result for each parent task
      for (int i = 0; i < l; i++) {

        // create a value vector and fill in the values for the
        // defined aspects ONLY.
        int[] types = ar.getAspectTypes();
        double acc[] = new double[types.length];
        for (int x = 0; x < types.length; x++) {
          // if the aspect is COST divide evenly across parents
          if (types[x] == AspectType.COST) {
            acc[x] = ar.getValue(types[x]) / l;
          } else if (types[x] == AspectType.QUANTITY) {
            // if the aspect is QUANTITY, we'll have to divide
            // proportionally across parents
            acc[x] = ar.getValue(types[x]) * quantProportions[i];
          } else {
            acc[x] = ar.getValue(types[x]);
          }
        }

        results[i] = new AllocationResult(ar.getConfidenceRating(),
                                          ar.isSuccess(),
                                          types,
                                          acc);

        // fill in the auxiliaryquery info
        // each of the new allocationresults(for the parents)
        // will have the SAME
        // auxiliaryquery info that the allocationresult (of the child) has.
        for (int aq = 0; aq < AuxiliaryQueryType.AQTYPE_COUNT; aq++) {
          String info = ar.auxiliaryQuery(aq);
          if (info != null) {
            results[i].addAuxiliaryQueryInfo(aq, info);
          }
        }
      }

      Task tasks[] = new Task[l];
      parents.copyInto(tasks);

      return new TaskScoreTable(tasks, results);
    }
  }

  // the following cribbed from LCG/CGI
  public double getTaskQuantity(Task task) {
    return getTaskAspectValue(task, AspectType.QUANTITY);
  } /* end method getTaskQuantity */

  /**
   * @return value of the preference on the aspect of the task, or
   * -1.0 if not defined.
   * @param at The aspect type.
   */
  protected static double getTaskAspectValue(Task task, int at) {
    //
    // Grab the designated aspect value.
    //
    double value = -1.0d;
    for (Enumeration preferences = task.getPreferences();
         preferences.hasMoreElements();) {
      Preference preference = (Preference) preferences.nextElement();
      if (preference.getAspectType() == at) {
        ScoringFunction sf = preference.getScoringFunction();
        AspectScorePoint asp = sf.getBest();
        AspectValue av = asp.getAspectValue();
        value = av.getValue();
      }
    }
    return (value);
  }

}





