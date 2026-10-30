#!/usr/bin/env python3
"""
공개 메서드 목록 어긋남 점검: spec/api-index.json <-> Python(ast) / Node(소스 파싱) / Java(소스 정규식) / spec/PUBLIC_DATA.md
점검 6개: 클래스의 공개 메서드 집합, 인자 이름·필수 여부·기본값, 반환 모델, Java 시그니처, 공개 데이터 모델 필드, PUBLIC_DATA.md 표
어긋나면 종료 코드 1 (CI: PROJECT-SEJONG-DOCS-CHECK), 원본은 spec/api-index.json 한 곳
"""
import ast
import json
import re
import sys
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parent.parent
API_INDEX_PATH = REPOSITORY_ROOT / "spec" / "api-index.json"
PUBLIC_DATA_SPEC_PATH = REPOSITORY_ROOT / "spec" / "PUBLIC_DATA.md"

PYTHON_PUBLIC_DATA_PATH = REPOSITORY_ROOT / "python/src/sejongpeia/public_data/client.py"
PYTHON_AUTH_PATH = REPOSITORY_ROOT / "python/src/sejongpeia/auth/client.py"
PYTHON_MODELS_PATH = REPOSITORY_ROOT / "python/src/sejongpeia/public_data/models.py"
NODE_PUBLIC_DATA_PATH = REPOSITORY_ROOT / "node/src/publicData/client.ts"
NODE_AUTH_PATH = REPOSITORY_ROOT / "node/src/auth/client.ts"
NODE_MODELS_PATH = REPOSITORY_ROOT / "node/src/publicData/models.ts"
JAVA_SOURCE_ROOT = REPOSITORY_ROOT / "java/src/main/java/kr/suhsaechan/sejongpeia"
JAVA_PUBLIC_DATA_PATH = JAVA_SOURCE_ROOT / "publicdata/SejongPublicData.java"
JAVA_AUTH_PATH = JAVA_SOURCE_ROOT / "auth/service/SuhSejongAuthEngine.java"
JAVA_MODEL_DIRECTORY = JAVA_SOURCE_ROOT / "publicdata/model"

# group > (Python 파일과 클래스, Node 파일과 클래스, Java 파일과 클래스)
CLASS_LOCATIONS_BY_GROUP = {
    "public_data": (PYTHON_PUBLIC_DATA_PATH, "SejongPublicData", NODE_PUBLIC_DATA_PATH, "SejongPublicData",
                    JAVA_PUBLIC_DATA_PATH, "SejongPublicData"),
    "auth": (PYTHON_AUTH_PATH, "SejongAuthClient", NODE_AUTH_PATH, "SejongAuthClient",
             JAVA_AUTH_PATH, "SuhSejongAuthEngine"),
}
NODE_STYLES = ("none", "positional", "options", "credentials-then-options")
NO_DEFAULT = object()


def camel_case_of(snake_name):
    head, *tail = snake_name.split("_")
    return head + "".join(word.capitalize() for word in tail)


def collapse_whitespace(text):
    return re.sub(r"\s+", " ", text).strip()


# ---------------- Python (ast)
def resolve_python_default(node, module_constants):
    """기본값 표현식 > 값 (모듈 상수 이름은 한 단계 풀어 줌)"""
    if isinstance(node, ast.Name) and node.id in module_constants:
        return module_constants[node.id]
    return ast.literal_eval(node)


def read_python_class(path, class_name):
    """Python 클래스 > {공개 메서드 이름: (인자 [(이름, 기본값 또는 NO_DEFAULT)], 반환 모델 이름 또는 None)}"""
    module = ast.parse(path.read_text(encoding="utf-8"))
    module_constants = {}
    for statement in module.body:
        if isinstance(statement, ast.Assign) and len(statement.targets) == 1 and isinstance(statement.targets[0], ast.Name):
            try:
                module_constants[statement.targets[0].id] = ast.literal_eval(statement.value)
            except ValueError:
                pass  # 리터럴이 아닌 모듈 상수(함수 호출 등)는 기본값 비교에 쓰지 않으므로 건너뜀
    class_node = next(node for node in module.body if isinstance(node, ast.ClassDef) and node.name == class_name)
    methods = {}
    for function in class_node.body:
        if not isinstance(function, ast.FunctionDef) or function.name.startswith("_"):
            continue
        arguments = function.args
        names = [argument.arg for argument in arguments.args][1:]  # self 제외
        default_nodes = [None] * (len(names) - len(arguments.defaults)) + list(arguments.defaults)
        parameters = [(name, NO_DEFAULT if default is None else resolve_python_default(default, module_constants))
                      for name, default in zip(names, default_nodes)]
        methods[function.name] = (parameters, python_return_model(function.returns))
    return methods


def python_return_model(annotation):
    """반환 어노테이션 `List[Term]` 또는 `Term` > (모델 이름, 목록 여부)"""
    if annotation is None:
        return None
    if isinstance(annotation, ast.Subscript):
        return (annotation.slice.id, True)
    return (annotation.id, False)


def read_python_model_fields(path):
    """models.py 데이터클래스 > {모델 이름: [필드 이름]}"""
    module = ast.parse(path.read_text(encoding="utf-8"))
    return {node.name: [item.target.id for item in node.body if isinstance(item, ast.AnnAssign)]
            for node in module.body if isinstance(node, ast.ClassDef)}


# ---------------- Node (TypeScript 소스 파싱)
def find_matching_close(text, open_index, open_char, close_char):
    depth = 0
    for index in range(open_index, len(text)):
        if text[index] == open_char:
            depth += 1
        elif text[index] == close_char:
            depth -= 1
            if depth == 0:
                return index
    raise ValueError("unbalanced " + open_char)


def split_top_level(text, separator=","):
    """괄호·중괄호·꺾쇠 안의 구분자는 건너뛰고 최상위에서만 나눔 (화살표 => 는 꺾쇠로 세지 않음)"""
    parts, depth, current = [], 0, []
    for index, char in enumerate(text):
        if char in "({[<":
            depth += 1
        elif char in ")}]" or (char == ">" and text[index - 1] != "="):
            depth -= 1
        if char == separator and depth == 0:
            parts.append("".join(current))
            current = []
        else:
            current.append(char)
    if "".join(current).strip():
        parts.append("".join(current))
    return [part.strip() for part in parts if part.strip()]


def read_node_interface_fields(source, interface_name, seen=None):
    """인터페이스 > [(필드 이름, 선택 여부)] (extends 도 따라감)"""
    match = re.search(r"interface\s+" + interface_name + r"(?:\s+extends\s+(\w+))?\s*\{", source)
    if not match:
        raise ValueError("interface not found: " + interface_name)
    body_end = find_matching_close(source, match.end() - 1, "{", "}")
    fields = []
    if match.group(1):
        fields += read_node_interface_fields(source, match.group(1))
    for member in re.split(r"[;\n]", source[match.end():body_end]):
        member = re.sub(r"/\*.*?\*/", "", member).strip()
        field = re.match(r"(\w+)(\?)?\s*:", member)
        if field:
            fields.append((field.group(1), bool(field.group(2))))
    return fields


def read_node_parameters(source, parameters_text):
    """매개변수 선언 > ([(이름, 선택 여부)] 평탄화, 구조 [positional 이름 또는 'options' ...])"""
    flat, shape = [], []
    for parameter in split_top_level(parameters_text):
        if parameter.startswith("{"):
            close = find_matching_close(parameter, 0, "{", "}")
            has_object_default = "=" in parameter[close + 1:]
            for entry in split_top_level(parameter[1:close]):
                name = re.match(r"(\w+)", entry).group(1)
                flat.append((name, "=" in entry or has_object_default))
            shape.append("options")
            continue
        name_match = re.match(r"(\w+)(\?)?\s*(?::\s*([^=]+?))?\s*(=.*)?$", parameter, re.DOTALL)
        name, is_optional, type_text, default_text = name_match.groups()
        type_name = (type_text or "").strip()
        if re.fullmatch(r"\w+Query", type_name):  # query: LecturesQuery > 인터페이스 필드로 풀기
            flat += [(field_name, is_optional_field) for field_name, is_optional_field
                     in read_node_interface_fields(source, type_name)]
            shape.append("options")
        else:
            flat.append((name, bool(is_optional or default_text)))
            shape.append(name)
    return flat, shape


def read_node_class(path, class_name):
    """Node 클래스 > {공개 메서드 이름: (평탄화 인자, 구조, 반환 모델 이름과 목록 여부)}"""
    source = path.read_text(encoding="utf-8")
    class_match = re.search(r"export class " + class_name + r"\b[^{]*\{", source)
    class_end = find_matching_close(source, class_match.end() - 1, "{", "}")
    body = source[class_match.end():class_end + 1]
    methods = {}
    for match in re.finditer(r"^  (?:async\s+)?(?!constructor|private|protected)(\w+)\(", body, re.MULTILINE):
        open_index = match.end() - 1
        close_index = find_matching_close(body, open_index, "(", ")")
        flat, shape = read_node_parameters(source, body[open_index + 1:close_index])
        return_match = re.match(r"\s*:\s*Promise<(\w+)(\[\])?>", body[close_index + 1:])
        if not return_match:
            continue  # 반환이 Promise 가 아닌 메서드는 공개 호출 대상 아님
        methods[match.group(1)] = (flat, shape, (return_match.group(1), bool(return_match.group(2))))
    return methods


def read_node_model_fields(path):
    """models.ts 인터페이스 > {모델 이름: [필드 이름]}"""
    source = path.read_text(encoding="utf-8")
    return {match.group(1): [name for name, _ in read_node_interface_fields(source, match.group(1))]
            for match in re.finditer(r"export interface (\w+)\b[^{]*\{", source)}


# ---------------- Java (정규식)
JAVA_METHOD_PATTERN = re.compile(r"^\s*public\s+(?!class\b)([\w<>\[\], ?.]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws [\w., ]+)?\{", re.MULTILINE)


def read_java_class(path):
    """Java 클래스 > [(메서드 이름, 정규화한 매개변수 선언, 반환 모델 이름과 목록 여부)] (생성자 제외)"""
    methods = []
    for match in JAVA_METHOD_PATTERN.finditer(path.read_text(encoding="utf-8")):
        return_text, name, parameters = match.groups()
        list_match = re.fullmatch(r"List<(\w+)>", return_text)
        return_model = (list_match.group(1), True) if list_match else (return_text, False)
        methods.append((name, collapse_whitespace(parameters), return_model))
    return methods


def read_java_model_fields(directory):
    """모델 클래스 파일 > {클래스 이름: [필드 이름]}"""
    return {path.stem: re.findall(r"private final [\w<>, ]+ (\w+);", path.read_text(encoding="utf-8"))
            for path in sorted(directory.glob("*.java"))}


# ---------------- PUBLIC_DATA.md 표
def read_spec_method_table(path):
    """`## Methods` 표 > {메서드 이름: [인자 이름]} (첫 열 `name(args)`)"""
    methods = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        match = re.match(r"\|\s*`(\w+)\((.*?)\)`\s*\|", line)
        if match:
            arguments = [re.match(r"\s*(\w+)", part).group(1) for part in split_top_level(match.group(2))]
            methods[match.group(1)] = arguments
    return methods


# ---------------- 비교
def find_problems():
    problems = []
    index = json.loads(API_INDEX_PATH.read_text(encoding="utf-8"))
    methods = index["methods"]
    python_model_fields = read_python_model_fields(PYTHON_MODELS_PATH)
    node_model_fields = read_node_model_fields(NODE_MODELS_PATH)
    java_model_fields = read_java_model_fields(JAVA_MODEL_DIRECTORY)

    for group, (python_path, python_class, node_path, node_class, java_path, java_class) in CLASS_LOCATIONS_BY_GROUP.items():
        group_methods = [method for method in methods if method["group"] == group]
        python_methods = read_python_class(python_path, python_class)
        node_methods = read_node_class(node_path, node_class)
        java_methods = read_java_class(java_path)

        # 공개 메서드 집합
        compare_sets(problems, f"[{group}] Python {python_class} public methods", {method["names"]["python"] for method in group_methods}, set(python_methods))
        compare_sets(problems, f"[{group}] Node {node_class} public methods", {method["names"]["node"] for method in group_methods}, set(node_methods))
        compare_sets(problems, f"[{group}] Java {java_class} public methods", {name for method in group_methods for name in method["names"]["java"]},
                     {name for name, _, _ in java_methods})

        for method in group_methods:
            check_python_method(problems, method, python_methods)
            check_node_method(problems, method, node_methods)
            check_java_method(problems, method, java_methods)
            check_return_model(problems, method, python_methods, node_methods, python_model_fields, is_auth_group=group == "auth")

    # 공개 데이터 모델 필드 (Python 기준, Node 는 camelCase 변환 후, Java 는 그대로)
    python_names = set(python_model_fields)
    compare_sets(problems, "public data models (Python vs Node)", python_names, set(node_model_fields))
    compare_sets(problems, "public data models (Python vs Java)", python_names, set(java_model_fields))
    for model_name, python_fields in python_model_fields.items():
        expected_camel = [camel_case_of(name) for name in python_fields]
        compare_sets(problems, f"model {model_name} fields (Python camelCase vs Node)", set(expected_camel), set(node_model_fields.get(model_name, [])))
        compare_sets(problems, f"model {model_name} fields (Python camelCase vs Java)", set(expected_camel), set(java_model_fields.get(model_name, [])))

    # spec/PUBLIC_DATA.md 표
    spec_methods = read_spec_method_table(PUBLIC_DATA_SPEC_PATH)
    public_data_methods = {method["names"]["python"]: method for method in methods if method["group"] == "public_data"}
    compare_sets(problems, "spec/PUBLIC_DATA.md Methods table vs api-index public_data", set(public_data_methods), set(spec_methods))
    for name, method in public_data_methods.items():
        if name in spec_methods and spec_methods[name] != [parameter["name"] for parameter in method["parameters"]]:
            problems.append(f"spec/PUBLIC_DATA.md: {name} arguments {spec_methods[name]} != api-index {[p['name'] for p in method['parameters']]}")
    problems += find_package_name_problems(index["packages"])
    return problems


def find_package_name_problems(packages):
    """패키지 이름 <-> pyproject.toml, node/package.json, 루트 build.gradle, Python 패키지 폴더"""
    problems = []
    pyproject = (REPOSITORY_ROOT / "python/pyproject.toml").read_text(encoding="utf-8")
    python_name = re.search(r'^name = "([^"]+)"', pyproject, re.MULTILINE).group(1)
    node_name = json.loads((REPOSITORY_ROOT / "node/package.json").read_text(encoding="utf-8"))["name"]
    java_group = re.search(r"^group = '([^']+)'", (REPOSITORY_ROOT / "build.gradle").read_text(encoding="utf-8"), re.MULTILINE).group(1)
    java_artifact = re.search(r"include '(\w+)'", (REPOSITORY_ROOT / "settings.gradle").read_text(encoding="utf-8"))
    java_artifact_name = java_artifact.group(1)
    for label, expected, actual in (
        ("Python package", packages["python"]["package"], python_name),
        ("Python import", packages["python"]["import"], "sejongpeia" if (REPOSITORY_ROOT / "python/src/sejongpeia").is_dir() else ""),
        ("Node package", packages["node"]["package"], node_name),
        ("Java groupId", packages["java"]["groupId"], java_group),
        ("Java artifactId", packages["java"]["artifactId"], java_artifact_name),
    ):
        if expected != actual:
            problems.append(f"packages: {label} api-index '{expected}' != code '{actual}'")
    return problems


def compare_sets(problems, label, expected, actual):
    if expected != actual:
        problems.append(f"{label}: only in api-index/expected {sorted(expected - actual)}, only in code {sorted(actual - expected)}")


def check_python_method(problems, method, python_methods):
    name = method["names"]["python"]
    if name not in python_methods:
        return
    parameters, _ = python_methods[name]
    expected = [(parameter["name"], parameter.get("default", NO_DEFAULT) if not parameter["required"] else NO_DEFAULT)
                for parameter in method["parameters"]]
    if [(n, d) for n, d in parameters] != expected:
        problems.append(f"Python {name}: signature {format_parameters(parameters)} != api-index {format_parameters(expected)}")


def format_parameters(parameters):
    return [name if default is NO_DEFAULT else f"{name}={default!r}" for name, default in parameters]


def check_node_method(problems, method, node_methods):
    name = method["names"]["node"]
    if name not in node_methods:
        return
    flat, shape, _ = node_methods[name]
    expected = [(camel_case_of(parameter["name"]), not parameter["required"]) for parameter in method["parameters"]]
    if flat != expected:
        problems.append(f"Node {name}: parameters {flat} != api-index {expected}")
    style = method["nodeStyle"]
    if style not in NODE_STYLES:
        problems.append(f"Node {name}: unknown nodeStyle {style}")
    actual_style = ("none" if not shape else "options" if shape == ["options"] else
                    "credentials-then-options" if shape[-1] == "options" and len(shape) == 3 else "positional")
    if actual_style != style:
        problems.append(f"Node {name}: call style {actual_style} != api-index nodeStyle {style}")


def check_java_method(problems, method, java_methods):
    expected = {collapse_whitespace(signature) for signature in method["javaSignatures"]}
    actual = {f"{name}({parameters})" for name, parameters, _ in java_methods if name in method["names"]["java"]}
    if expected != actual:
        problems.append(f"Java {method['id']}: signatures only in api-index {sorted(expected - actual)}, only in code {sorted(actual - expected)}")
    for name, _, return_model in java_methods:
        if name in method["names"]["java"] and return_model != (method["returns"]["model"], method["returns"]["isList"]):
            problems.append(f"Java {name}: returns {return_model} != api-index {method['returns']}")


def check_return_model(problems, method, python_methods, node_methods, python_model_fields, is_auth_group):
    expected = (method["returns"]["model"], method["returns"]["isList"])
    python_name, node_name = method["names"]["python"], method["names"]["node"]
    if python_name in python_methods and python_methods[python_name][1] != expected:
        problems.append(f"Python {python_name}: returns {python_methods[python_name][1]} != api-index {expected}")
    if node_name in node_methods and node_methods[node_name][2] != expected:
        problems.append(f"Node {node_name}: returns {node_methods[node_name][2]} != api-index {expected}")
    if not is_auth_group and method["returns"]["model"] not in python_model_fields:
        problems.append(f"api-index {method['id']}: model {method['returns']['model']} is not a public data model")


def main():
    problems = find_problems()
    for problem in problems:
        print(f"[api index] {problem}")
    method_count = len(json.loads(API_INDEX_PATH.read_text(encoding="utf-8"))["methods"])
    print(f"[api index] {method_count} methods, {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
