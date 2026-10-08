package main

import "C"

import (
	"encoding/json"
	"reflect"
)

func marshalJson(obj any) *C.char {
	res, err := json.Marshal(obj)
	if err != nil {
		// cgo 导出函数里 panic 会直接崩掉进程；序列化失败回退空 JSON，
		// 让 Kotlin 侧拿到可解析的空对象而不是 VM abort
		res = []byte("{}")
	}

	return C.CString(string(res))
}

func marshalString(obj any) *C.char {
	if obj == nil {
		return nil
	}

	switch o := obj.(type) {
	case error:
		return C.CString(o.Error())
	case string:
		return C.CString(o)
	}

	panic("invalid marshal type " + reflect.TypeOf(obj).Name())
}
