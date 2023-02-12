package waffleoRai_AutoCode;

import java.util.Map;

import waffleoRai_AutoCode.typedefs.AnonStructFieldDef;
import waffleoRai_AutoCode.typedefs.ArrayFieldDef;
import waffleoRai_AutoCode.typedefs.BasicDataFieldDef;
import waffleoRai_AutoCode.typedefs.BingennerTypedef;
import waffleoRai_AutoCode.typedefs.BitFieldDef;
import waffleoRai_AutoCode.typedefs.DataFieldDef;
import waffleoRai_AutoCode.typedefs.EnumDef;
import waffleoRai_AutoCode.typedefs.ListDef;

public interface BingennerTarget {

	public void addPrimitive(BasicDataFieldDef data_field);
	public void addPrimitiveArray(ArrayFieldDef data_field);
	public void addBitfield(BitFieldDef def);
	public void addEnum(BasicDataFieldDef data_field);
	public void addTable(ListDef def);
	public void addStruct(DataFieldDef data_field, String typename, boolean asPointer);
	public void addAnonStruct(AnonStructFieldDef struct_info);
	public void addStructArray(ArrayFieldDef data_field, String typename, boolean asPointer);
	
	public void resolveImports(Map<String, BingennerTypedef> type_map);
	public void resolveEnumImports(Map<String, EnumDef> enum_map);
}
