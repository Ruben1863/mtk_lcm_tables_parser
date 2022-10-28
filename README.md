# MTK LCM TABLES Parser by ruben1863

## What is this?
This is a decoding/reversing utility that is able to extract MTK lcm drivers init table from a binary file (kernel or lk.bin) providing its start header.
The binary file should be placed at `input` folder, and possible lcm tables will be placed at `output` folder.
The output tables will be formatted using `struct LCM_setting_table` syntax.

Example:
```
Your header is: F0025A5A

The output will be like:
	{0xF0, 2, {0x5A, 0x5A}},
	/*
		more data here
	*/
	{REGFLAG_END_OF_TABLE, 0, {}}
```

## Notes: 
- This utility it is still in alpha version, so it is very possible that you will find errors or certain tables that it cannot process.
- For now is just able to extract `lcm_init` tables. 
- V3 init tables aren't supported.
- In case something is broken or you have any doubt, feel free to open an [ISSUE](https://github.com/Ruben1863/mtk_lcm_tables_parser/issues "Issues").

## Requirements
* Java 11 (Minimum)

## Usage:
Code:
```
java -jar parser.jar -i <header> -f <filename>
```

Arguments:
```
Required arguments:
  -i, --input			Table header without spaces (like: FF0000000101)
  -f, --file			Input file name (it should be placed at 'input' directory)
  
Optional arguments:
  -s, --silent			Suppresses text output
  -h, --help			Prints this menu
```

## License
This tool is licensed under the GNU General Public License (V3). See [LICENSE](https://github.com/Ruben1863/mtk_lcm_tables_parser/blob/main/LICENSE) for more details.