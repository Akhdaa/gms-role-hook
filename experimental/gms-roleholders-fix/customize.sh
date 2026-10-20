#!/system/bin/sh
# Files must be root:root 0644 or the SystemConfig parser may skip them.
set_perm_recursive "$MODPATH/system" 0 0 0755 0644
ui_print "- GMS Role Holders Fix installed. Reboot to apply."
