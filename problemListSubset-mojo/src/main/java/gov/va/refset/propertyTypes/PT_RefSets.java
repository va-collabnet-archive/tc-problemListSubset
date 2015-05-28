package gov.va.refset.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_MemberRefsets;

/**
 * Properties from the DTS ndf load which are treated as alternate IDs within the workbench.
 * @author Daniel Armbrust
 */
public class PT_RefSets extends BPT_MemberRefsets
{
	public PT_RefSets()
	{
		super("VA/KP Problem List");
		addProperty("VA/KP Problem List");
	}
}
