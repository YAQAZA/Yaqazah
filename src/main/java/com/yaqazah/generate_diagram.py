import os
import re

def parse_java_file(filepath):
    class_name = None
    is_interface = False
    methods = []
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
    # Remove comments
    content = re.sub(r'//.*', '', content)
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    
    class_match = re.search(r'(?:public\s+)?(?:class|interface)\s+(\w+)', content)
    if class_match:
        class_name = class_match.group(1)
        if 'interface' in class_match.group(0):
            is_interface = True
    else:
        return None
        
    # extract methods properly handling newlines
    method_matches = re.finditer(r'(?:public|protected)\s+((?:[\w<>,\?\[\]]+\s+)+)(\w+)\s*\((.*?)\)', content, re.DOTALL)
    for m in method_matches:
        rtype_str = m.group(1).strip()
        rtype = rtype_str.split()[-1] # the last token before the method name
        mname = m.group(2).strip()
        args_raw = m.group(3).strip()
        
        # strip annotations from args
        args_raw = re.sub(r'@\w+(?:\([^)]*\))?\s*', '', args_raw)
        
        args_clean = []
        if args_raw:
            for arg in args_raw.split(','):
                parts = arg.strip().split()
                if len(parts) >= 2:
                    args_clean.append(parts[-2] + ' ' + parts[-1])
                elif len(parts) == 1:
                    args_clean.append(parts[0])
        args_str = ', '.join(args_clean)
        
        if mname not in ['class', 'interface'] and class_name != mname: 
            # also skip constructors
            methods.append(f"+{rtype} {mname}({args_str})")
            
    return {
        'name': class_name,
        'is_interface': is_interface,
        'methods': methods
    }

def main():
    directory = '.'
    target_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(('Controller.java', 'Service.java', 'Repository.java')):
                target_files.append(os.path.join(root, file))
                
    mermaid = ["classDiagram", "    direction TB"]
    
    for f in target_files:
        data = parse_java_file(f)
        if data and data['name']:
            mermaid.append(f"    class {data['name']} {{")
            if data['is_interface']:
                mermaid.append("        <<interface>>")
            # De-duplicate methods
            seen = set()
            for method in data['methods']:
                if method not in seen:
                    # Clean up problematic mermaid chars
                    clean_m = method.replace('<', '~').replace('>', '~')
                    mermaid.append(f"        {clean_m}")
                    seen.add(method)
            mermaid.append("    }")
            
    with open('mermaid_diagram.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(mermaid))
        
if __name__ == '__main__':
    main()
