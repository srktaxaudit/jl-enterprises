import * as LocalAuthentication from "expo-local-authentication";

/** True only when the device has biometric hardware AND an enrolled biometric. */
export async function biometricAvailable(): Promise<boolean> {
  try {
    const [hasHardware, enrolled] = await Promise.all([
      LocalAuthentication.hasHardwareAsync(),
      LocalAuthentication.isEnrolledAsync(),
    ]);
    return hasHardware && enrolled;
  } catch {
    return false;
  }
}

export async function biometricUnlock(reason = "Unlock JL Admin"): Promise<boolean> {
  try {
    const res = await LocalAuthentication.authenticateAsync({
      promptMessage: reason,
      fallbackLabel: "Use device passcode",
      disableDeviceFallback: false,
    });
    return res.success;
  } catch {
    return false;
  }
}
