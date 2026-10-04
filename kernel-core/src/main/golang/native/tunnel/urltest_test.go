package tunnel

import (
	"errors"
	"fmt"
	"testing"
)

func TestClassifyUrlTestError(t *testing.T) {
	cases := []struct {
		name string
		err  error
		want string
	}{
		{"nil 错误返回空分类", nil, ""},
		{"全小写 refused 判离线", errors.New("dial tcp 1.2.3.4:443: connect: connection refused"), "offline"},
		{"大小写混合 refused 也判离线", errors.New("Dial TCP: Connect: Connection Refused"), "offline"},
		{"no such host 判离线", errors.New("lookup example.invalid: no such host"), "offline"},
		{"no route 判离线", errors.New("connect: no route to host"), "offline"},
		{"network unreachable 判离线", errors.New("socket: network is unreachable"), "offline"},
		{"TLS 异常保守按超时", errors.New("tls: handshake failure"), "timeout"},
		{"上下文超时按超时", errors.New("context deadline exceeded"), "timeout"},
		{"包裹错误也能分类", fmt.Errorf("dial: %w", errors.New("connection refused")), "offline"},
	}
	for _, tc := range cases {
		if got := classifyUrlTestError(tc.err); got != tc.want {
			t.Errorf("%s: classifyUrlTestError(%v) = %q, want %q", tc.name, tc.err, got, tc.want)
		}
	}
}
