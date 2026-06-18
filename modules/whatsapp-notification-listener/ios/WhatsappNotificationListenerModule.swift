import ExpoModulesCore

public class WhatsappNotificationListenerModule: Module {
  public func definition() -> ModuleDefinition {
    Name("WhatsappNotificationListener")
    
    Function("isPermissionGranted") { () -> Bool in
      return false
    }
    
    Function("requestPermission") { () -> Void in
      // No-op on iOS
    }
    
    Function("setAnnouncementEnabled") { (enabled: Bool) -> Void in
      // No-op
    }
    
    Function("isAnnouncementEnabled") { () -> Bool in
      return true
    }
    
    Function("speak") { (text: String) -> Void in
      // No-op
    }
  }
}
