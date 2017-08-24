/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.7
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua2;

public final class pjsip_evsub_state {
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_NULL = new pjsip_evsub_state("PJSIP_EVSUB_STATE_NULL");
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_SENT = new pjsip_evsub_state("PJSIP_EVSUB_STATE_SENT");
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_ACCEPTED = new pjsip_evsub_state("PJSIP_EVSUB_STATE_ACCEPTED");
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_PENDING = new pjsip_evsub_state("PJSIP_EVSUB_STATE_PENDING");
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_ACTIVE = new pjsip_evsub_state("PJSIP_EVSUB_STATE_ACTIVE");
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_TERMINATED = new pjsip_evsub_state("PJSIP_EVSUB_STATE_TERMINATED");
  public final static pjsip_evsub_state PJSIP_EVSUB_STATE_UNKNOWN = new pjsip_evsub_state("PJSIP_EVSUB_STATE_UNKNOWN");

  public final int swigValue() {
    return swigValue;
  }

  public String toString() {
    return swigName;
  }

  public static pjsip_evsub_state swigToEnum(int swigValue) {
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (int i = 0; i < swigValues.length; i++)
      if (swigValues[i].swigValue == swigValue)
        return swigValues[i];
    throw new IllegalArgumentException("No enum " + pjsip_evsub_state.class + " with value " + swigValue);
  }

  private pjsip_evsub_state(String swigName) {
    this.swigName = swigName;
    this.swigValue = swigNext++;
  }

  private pjsip_evsub_state(String swigName, int swigValue) {
    this.swigName = swigName;
    this.swigValue = swigValue;
    swigNext = swigValue+1;
  }

  private pjsip_evsub_state(String swigName, pjsip_evsub_state swigEnum) {
    this.swigName = swigName;
    this.swigValue = swigEnum.swigValue;
    swigNext = this.swigValue+1;
  }

  private static pjsip_evsub_state[] swigValues = { PJSIP_EVSUB_STATE_NULL, PJSIP_EVSUB_STATE_SENT, PJSIP_EVSUB_STATE_ACCEPTED, PJSIP_EVSUB_STATE_PENDING, PJSIP_EVSUB_STATE_ACTIVE, PJSIP_EVSUB_STATE_TERMINATED, PJSIP_EVSUB_STATE_UNKNOWN };
  private static int swigNext = 0;
  private final int swigValue;
  private final String swigName;
}

