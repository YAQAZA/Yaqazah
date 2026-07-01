import os
import re

def parse_java_file(filepath):
    class_name = None
    dependencies = []
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
        
    content = re.sub(r'//.*', '', content)
    content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
    
    class_match = re.search(r'(?:public\s+)?(?:class|interface)\s+(\w+)', content)
    if class_match:
        class_name = class_match.group(1)
    else:
        return None
        
    field_matches = re.finditer(r'(?:private|protected)(?:\s+final)?\s+([A-Z]\w*(?:Service|Repository|Controller))\s+\w+', content)
    for m in field_matches:
        dep = m.group(1).strip()
        if dep != class_name:
            dependencies.append(dep)
            
    return {
        'name': class_name,
        'dependencies': list(set(dependencies))
    }

def main():
    directory = '.'
    target_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(('Controller.java', 'Service.java', 'Repository.java')):
                target_files.append(os.path.join(root, file))
                
    all_dependencies = []
    for f in target_files:
        data = parse_java_file(f)
        if data and data['name']:
            for dep in data['dependencies']:
                all_dependencies.append(f"    {data['name']} --> {dep}")
                
    with open('mermaid_links.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(sorted(list(set(all_dependencies)))))
        
if __name__ == '__main__':
    main()
