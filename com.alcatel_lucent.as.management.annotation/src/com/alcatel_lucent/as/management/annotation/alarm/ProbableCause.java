package com.alcatel_lucent.as.management.annotation.alarm ;

/**
 * X.733 Alarm probable causes
 */
public enum ProbableCause {
  adapterError(1),
  applicationSubsystemFailure(2),
  bandwidthReduced(3),
  callEstablishmentError(4),
  communicationsProtocolError(5),
  communicationsSubsystemFailure(6),
  configurationOrCustomizationError(7),
  congestion(8),
  corruptData(9),
  cpuCyclesLimitExceeded(10),
  dataSetOrModemError(11),
  degradedSignal(12),
  dTEdCEInterfaceError(13),
  enclosureDoorOpen(14),
  equipmentMalfunction(15),
  excessiveVibration(16),
  fileError(17),
  fireDetected(18),
  floodDetected(19),
  framingError(20),
  heatingOrVentilationOrCoolingSystemProblem(21),
  humidityUnacceptable(22),
  inputOutputDeviceError(23),
  inputDeviceError(24),
  lANError(25),
  leakDetected(26),
  localNodeTransmissionError(27),
  lossOfFrame(28),
  lossOfSignal(29),
  materialSupplyExhausted(30),
  multiplexerProblem(31),
  outOfMemory(32),
  ouputDeviceError(33),
  performanceDegraded(34),
  powerProblem(35),
  pressureUnacceptable(36),
  processorProblem(37),
  pumpFailure(38),
  queueSizeExceeded(39),
  receiveFailure(40),
  receiverFailure(41),
  remoteNodeTransmissionError(42),
  resourceAtOrNearingCapacity(43),
  responseTimeExcessive(44),
  retransmissionRateExcessive(45),
  softwareError(46),
  softwareProgramAbnormallyTerminated(47),
  softwareProgramError(48),
  storageCapacityProblem(49),
  temperatureUnacceptable(50),
  thresholdCrossed(51),
  timingProblem(52),
  toxicLeakDetected(53),
  transmitFailure(54),
  transmitterFailure(55),
  underlyingResourceUnavailable(56),
  versionMismatch(57),
  authenticationFailure(58),
  breachOfConfidentiality(59),
  cableTamper(60),
  delayedInformation(61),
  denialOfService(62),
  duplicateInformation(63),
  informationMissing(64),
  informationModificationDetected(65),
  informationOutOfSequence(66),
  intrusionDetection(67),
  keyExpired(68),
  nonRepudiationFailure(69),
  outOfHoursActivity(70),
  outOfService(71),
  proceduralError(72),
  unauthorizedAccessAttempt(73),
  unexpectedInformation(74),
  unspecifiedReason(75) ;

  /** Default probable cause */
  public static final ProbableCause DEFAULT = ProbableCause.applicationSubsystemFailure ;

  /**
   * Get an instance from a string. The string is either a name or an integer
   * @param s String to decode
   * @return Object instance, null if not found
   */
  public static ProbableCause get (String s) {
    try {
      return getFromValue (Integer.parseInt (s)) ;
    } catch (Throwable t) {
    }
    for (ProbableCause object : values()) {
      if (object.name().equals (s)) {
        return object ;
      }
    }
    return null ;
  }

  /**
   * Get an instance from a value
   * @param value Value to lookup
   * @return Object instance, null if not found
   */
  public static ProbableCause getFromValue (int value) {
    for (ProbableCause object : values()) {
      if (object.getValue() == value) {
        return object ;
      }
    }
    return null ;
  }

  /** Associated value */
  private int value ;

  /**
   * Constructor from value
   * @param value Value
   */
  private ProbableCause (int value) {
    this.value = value ;
  }

  /**
   * Retrieve the alarm type
   * @return Alarm type
   */
  public int getValue() {
    return value ;
  }
}
