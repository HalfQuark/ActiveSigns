# ActiveSigns
ActiveSigns is a Movecraft addon which allows redstone signals to interact with movecraft signs.
# Signal
  A sign will be activated if either an adjacent redstone dust is powered, redstone repeater pointing into it is powered or an adjacent redstone torch is powered.
  Depending on the sign placement and input it will either simulate a left-click or right-click:
- Wall signs  
  These will always be right-clicked when activated.
- Standing signs  
  These will be left-clicked if activated from the left or from behind and right-clicked if activated from the right or from the front.
# Blacklist
  Any sign containing a string from the editable blacklist section of the config.yml will not be activated by redstone.
# Whitelist
  If this list is not empty only signs containing all the strings in the whitelist will be activated (if they don't contain any blacklisted string).
# Output
  Some signs also give redstone outputs in various signal strengths.  
  To get an output from a sign place a redstone dust adjacent to the sign.  
- Status Sign
  If it is a wall sign it will output the lift percent of the craft.  
    Signal strength = min(15, Wool% - Required Wool% + 1)  

  If it is a standing sign it will output the redstone percent of the craft.  
    Signal strength = min(15, Redstone% - Required Redstone%)  

- Speed Sign
  Any sign type will output the current speed of the craft.  
    Signal strength = min(15, Speed(m/s) / 0.25)  

- Contacts Sign
  If it is a wall sign it will output the distance to the nearest craft from the center of the ship.  
    Signal strength = min(15, 300 / distance)  

  If it is a standing sign it will output for each adjacent dust the least orthogonal distance to a craft on the respective positive/negative axis.  
    Signal strength = min(15, 300 / distance)  
