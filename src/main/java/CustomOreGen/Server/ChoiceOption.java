package CustomOreGen.Server;

import java.util.LinkedHashMap;

import CustomOreGen.Util.CIStringMap;
import CustomOreGen.Util.Localization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ChoiceOption extends ConfigOption
{
    private String _value = null;
    private CIStringMap<String[]> _valueMap = new CIStringMap<String[]>(new LinkedHashMap<String,String[]>());

    public ChoiceOption(String name)
    {
        super(name);
    }

    public Object getValue()
    {
        return this._value;
    }

    public boolean setValue(Object value)
    {
        if (value == null)
        {
            return false;
        }
        else if (!(value instanceof String))
        {
            return false;
        }
        else
        {
            String canonicalValue = this._valueMap.getCanonicalKey((String)value);

            if (canonicalValue == null)
            {
                return false;
            }
            else
            {
                this._value = canonicalValue;
                return true;
            }
        }
    }

    public void addPossibleValue(String value, String displayValue, String description)
    {
        if (value != null)
        {
            this._valueMap.put(value, new String[] {displayValue, description});

            if (this._value == null)
            {
                this._value = value;
            }
        }
    }

    public void removePossibleValue(String value)
    {
        if (value != null)
        {
            if (this._value == value)
            {
                this._value = this.nextPossibleValue();

                if (this._value == value)
                {
                    this._value = null;
                }
            }

            this._valueMap.remove(value);
        }
    }

    public String nextPossibleValue()
    {
        boolean found = false;
        String first = null;
        
        for (String s : this._valueMap.keySet()) {
        	
            if (first == null)
            {
                first = s;
            }

            if (found)
            {
                return s;
            }

            if (s.equals(this._value))
            {
                found = true;
            }
        }

        if (found)
        {
            return first;
        }
        else
        {
            return null;
        }
    }

    public String getDisplayValue()
    {
        String[] display = this._valueMap.get(this._value);
        return display != null && display[0] != null ? display[0] : this._value;
    }

    public String getValueDescription()
    {
        String[] display = this._valueMap.get(this._value);
        return display == null ? null : display[1];
    }

    @OnlyIn(Dist.CLIENT)
    public String getLocalizedValueDescription() {
    	return this.localize("description", this.getValueDescription());
    }

    @OnlyIn(Dist.CLIENT)
    public String getLocalizedDisplayValue() {
    	return this.localize("displayValue", this.getDisplayValue());
    }

    @OnlyIn(Dist.CLIENT)
	private String localize(String key, String value) {
		return Localization.maybeLocalize(this.getName() + "." + this.getValue() + "." + key, value); 
	}
}
