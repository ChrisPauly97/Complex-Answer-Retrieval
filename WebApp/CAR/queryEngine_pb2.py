# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: queryEngine.proto

import sys

_b = sys.version_info[0] < 3 and (lambda x: x) or (lambda x: x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()

DESCRIPTOR = _descriptor.FileDescriptor(
    name='queryEngine.proto',
    package='queryEngine',
    syntax='proto3',
    serialized_options=_b('\n\026com.carapi.queryEngineB\020queryEngineProtoP\001\242\002\003RTG'),
    serialized_pb=_b(
        '\n\x11queryEngine.proto\x12\x0bqueryEngine\"Q\n\x0cQueryRequest\x12\x14\n\x0crawQueryText\x18\x01 \x01(\t\x12\x15\n\roriginalQuery\x18\x02 \x01(\t\x12\x14\n\x0cnumberOfDocs\x18\x03 \x01(\x05\"r\n\rQueryResponse\x12$\n\x07Results\x18\x01 \x03(\x0b\x32\x13.queryEngine.result\x12$\n\tqueryEval\x18\x02 \x01(\x0b\x32\x11.queryEngine.eval\x12\x15\n\rqrelParagraph\x18\x03 \x03(\t\"/\n\x06result\x12\x11\n\tparagraph\x18\x01 \x01(\t\x12\x12\n\nsimilarity\x18\x02 \x01(\t\"0\n\x04\x65val\x12\x0b\n\x03map\x18\x03 \x01(\x02\x12\r\n\x05rprec\x18\x04 \x01(\x02\x12\x0c\n\x04ndcg\x18\x05 \x01(\x02\x32S\n\x0bQueryEngine\x12\x44\n\tsendQuery\x12\x19.queryEngine.QueryRequest\x1a\x1a.queryEngine.QueryResponse\"\x00\x42\x32\n\x16\x63om.carapi.queryEngineB\x10queryEngineProtoP\x01\xa2\x02\x03RTGb\x06proto3')
)

_QUERYREQUEST = _descriptor.Descriptor(
    name='QueryRequest',
    full_name='queryEngine.QueryRequest',
    filename=None,
    file=DESCRIPTOR,
    containing_type=None,
    fields=[
        _descriptor.FieldDescriptor(
            name='rawQueryText', full_name='queryEngine.QueryRequest.rawQueryText', index=0,
            number=1, type=9, cpp_type=9, label=1,
            has_default_value=False, default_value=_b("").decode('utf-8'),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='originalQuery', full_name='queryEngine.QueryRequest.originalQuery', index=1,
            number=2, type=9, cpp_type=9, label=1,
            has_default_value=False, default_value=_b("").decode('utf-8'),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='numberOfDocs', full_name='queryEngine.QueryRequest.numberOfDocs', index=2,
            number=3, type=5, cpp_type=1, label=1,
            has_default_value=False, default_value=0,
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
    ],
    extensions=[
    ],
    nested_types=[],
    enum_types=[
    ],
    serialized_options=None,
    is_extendable=False,
    syntax='proto3',
    extension_ranges=[],
    oneofs=[
    ],
    serialized_start=34,
    serialized_end=115,
)

_QUERYRESPONSE = _descriptor.Descriptor(
    name='QueryResponse',
    full_name='queryEngine.QueryResponse',
    filename=None,
    file=DESCRIPTOR,
    containing_type=None,
    fields=[
        _descriptor.FieldDescriptor(
            name='Results', full_name='queryEngine.QueryResponse.Results', index=0,
            number=1, type=11, cpp_type=10, label=3,
            has_default_value=False, default_value=[],
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='queryEval', full_name='queryEngine.QueryResponse.queryEval', index=1,
            number=2, type=11, cpp_type=10, label=1,
            has_default_value=False, default_value=None,
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='qrelParagraph', full_name='queryEngine.QueryResponse.qrelParagraph', index=2,
            number=3, type=9, cpp_type=9, label=3,
            has_default_value=False, default_value=[],
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
    ],
    extensions=[
    ],
    nested_types=[],
    enum_types=[
    ],
    serialized_options=None,
    is_extendable=False,
    syntax='proto3',
    extension_ranges=[],
    oneofs=[
    ],
    serialized_start=117,
    serialized_end=231,
)

_RESULT = _descriptor.Descriptor(
    name='result',
    full_name='queryEngine.result',
    filename=None,
    file=DESCRIPTOR,
    containing_type=None,
    fields=[
        _descriptor.FieldDescriptor(
            name='paragraph', full_name='queryEngine.result.paragraph', index=0,
            number=1, type=9, cpp_type=9, label=1,
            has_default_value=False, default_value=_b("").decode('utf-8'),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='similarity', full_name='queryEngine.result.similarity', index=1,
            number=2, type=9, cpp_type=9, label=1,
            has_default_value=False, default_value=_b("").decode('utf-8'),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
    ],
    extensions=[
    ],
    nested_types=[],
    enum_types=[
    ],
    serialized_options=None,
    is_extendable=False,
    syntax='proto3',
    extension_ranges=[],
    oneofs=[
    ],
    serialized_start=233,
    serialized_end=280,
)

_EVAL = _descriptor.Descriptor(
    name='eval',
    full_name='queryEngine.eval',
    filename=None,
    file=DESCRIPTOR,
    containing_type=None,
    fields=[
        _descriptor.FieldDescriptor(
            name='map', full_name='queryEngine.eval.map', index=0,
            number=3, type=2, cpp_type=6, label=1,
            has_default_value=False, default_value=float(0),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='rprec', full_name='queryEngine.eval.rprec', index=1,
            number=4, type=2, cpp_type=6, label=1,
            has_default_value=False, default_value=float(0),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
        _descriptor.FieldDescriptor(
            name='ndcg', full_name='queryEngine.eval.ndcg', index=2,
            number=5, type=2, cpp_type=6, label=1,
            has_default_value=False, default_value=float(0),
            message_type=None, enum_type=None, containing_type=None,
            is_extension=False, extension_scope=None,
            serialized_options=None, file=DESCRIPTOR),
    ],
    extensions=[
    ],
    nested_types=[],
    enum_types=[
    ],
    serialized_options=None,
    is_extendable=False,
    syntax='proto3',
    extension_ranges=[],
    oneofs=[
    ],
    serialized_start=282,
    serialized_end=330,
)

_QUERYRESPONSE.fields_by_name['Results'].message_type = _RESULT
_QUERYRESPONSE.fields_by_name['queryEval'].message_type = _EVAL
DESCRIPTOR.message_types_by_name['QueryRequest'] = _QUERYREQUEST
DESCRIPTOR.message_types_by_name['QueryResponse'] = _QUERYRESPONSE
DESCRIPTOR.message_types_by_name['result'] = _RESULT
DESCRIPTOR.message_types_by_name['eval'] = _EVAL
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

QueryRequest = _reflection.GeneratedProtocolMessageType('QueryRequest', (_message.Message,), dict(
    DESCRIPTOR=_QUERYREQUEST,
    __module__='queryEngine_pb2'
    # @@protoc_insertion_point(class_scope:queryEngine.QueryRequest)
))
_sym_db.RegisterMessage(QueryRequest)

QueryResponse = _reflection.GeneratedProtocolMessageType('QueryResponse', (_message.Message,), dict(
    DESCRIPTOR=_QUERYRESPONSE,
    __module__='queryEngine_pb2'
    # @@protoc_insertion_point(class_scope:queryEngine.QueryResponse)
))
_sym_db.RegisterMessage(QueryResponse)

result = _reflection.GeneratedProtocolMessageType('result', (_message.Message,), dict(
    DESCRIPTOR=_RESULT,
    __module__='queryEngine_pb2'
    # @@protoc_insertion_point(class_scope:queryEngine.result)
))
_sym_db.RegisterMessage(result)

eval = _reflection.GeneratedProtocolMessageType('eval', (_message.Message,), dict(
    DESCRIPTOR=_EVAL,
    __module__='queryEngine_pb2'
    # @@protoc_insertion_point(class_scope:queryEngine.eval)
))
_sym_db.RegisterMessage(eval)

DESCRIPTOR._options = None

_QUERYENGINE = _descriptor.ServiceDescriptor(
    name='QueryEngine',
    full_name='queryEngine.QueryEngine',
    file=DESCRIPTOR,
    index=0,
    serialized_options=None,
    serialized_start=332,
    serialized_end=415,
    methods=[
        _descriptor.MethodDescriptor(
            name='sendQuery',
            full_name='queryEngine.QueryEngine.sendQuery',
            index=0,
            containing_service=None,
            input_type=_QUERYREQUEST,
            output_type=_QUERYRESPONSE,
            serialized_options=None,
        ),
    ])
_sym_db.RegisterServiceDescriptor(_QUERYENGINE)

DESCRIPTOR.services_by_name['QueryEngine'] = _QUERYENGINE

# @@protoc_insertion_point(module_scope)
