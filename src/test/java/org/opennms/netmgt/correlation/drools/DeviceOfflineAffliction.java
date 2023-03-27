package org.opennms.netmgt.correlation.drools;

import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.drools.core.common.DefaultFactHandle;

public class DeviceOfflineAffliction extends DefaultFactHandle {

  String location;
  ConcurrentSkipListSet offlineDevices;

  public String getLocation() {
    return location;
  }

  public DeviceOfflineAffliction setLocation(String location) {
    this.location = location;
    return this;
  }

  public ConcurrentSkipListSet getOfflineDevices() {
    return offlineDevices;
  }

  public DeviceOfflineAffliction() {}

  public DeviceOfflineAffliction setOfflineDevices(ConcurrentSkipListSet offlineDevices) {
    this.offlineDevices = offlineDevices;
    return this;
  }

  public DeviceOfflineAffliction(String location, ConcurrentSkipListSet offlineDevices) {
    this.location = location;
    this.offlineDevices = offlineDevices;
  }

  @Override
  public String toString() {
    return "DeviceOfflineAffliction{"
        + "location='"
        + location
        + '\''
        + ", offlineDevices="
        + offlineDevices
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DeviceOfflineAffliction)) return false;

    DeviceOfflineAffliction that = (DeviceOfflineAffliction) o;

    return new EqualsBuilder()
        .append(getLocation(), that.getLocation())
        .append(getOfflineDevices(), that.getOfflineDevices())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(getLocation())
        .append(getOfflineDevices())
        .toHashCode();
  }
}
