反射三定律：

​    1.反射可以将“接口类型变量”转换为“反射类型对象”。

​    2.反射可以将“反射类型对象”转换为“接口类型变量”。

​    3.如果要修改“反射类型对象”，其值必须是“可写的”（settable）。



go语言中interface底层实现是有两个struc实现的，分别是eface和iface

​		eface表示无方法的interface

​		iface表示有方法的interface

```
//位于go/src/runtime/runtime2.go中

type iface struct {
	tab  *itab
	data unsafe.Pointer
}

// layout of Itab known to compilers
// allocated in non-garbage-collected memory
// Needs to be in sync with
// ../cmd/compile/internal/gc/reflect.go:/^func.dumptypestructs.
type itab struct {
	inter *interfacetype
	_type *_type
	hash  uint32 // copy of _type.hash. Used for type switches.
	_     [4]byte
	fun   [1]uintptr // variable sized. fun[0]==0 means _type does not implement inter.
}

type eface struct {
	_type *_type
	data  unsafe.Pointer
}
```

查看源码可以发现，这两个struct都包含两个相同的属性(_type *_type)和（data  unsafe.Pointer）分别用来表示interface的静态类型和指向保存具体data的地址。



了解了这一点就可以理解第一大法则了，**反射可以将“接口类型变量”转换为“反射类型对象”**

查看reflect.Value和reflect.Type，就可以发现，这两个方法的入参都是interface{}，golang中所有对象都可以看做是继承了interface，在调用这两个方法时，首先会把具体的变量转换成interface，然后再操作interface中的type和data来得到具体的反射类型对象。

查看reflect.TypeOf()方法

```
// TypeOf returns the reflection Type that represents the dynamic type of i.
// If i is a nil interface value, TypeOf returns nil.
func TypeOf(i interface{}) Type {
	eface := *(*emptyInterface)(unsafe.Pointer(&i))
	return toType(eface.typ)
}
//来看看emptyInterface是个什么东西，可以看到emptyInterface和nonEmptyInterface分别跟eface和iface能够对应上，


//rtype实现了Type的所有方法
// toType converts from a *rtype to a Type that can be returned
// to the client of package reflect. In gc, the only concern is that
// a nil *rtype must be replaced by a nil Type, but in gccgo this
// function takes care of ensuring that multiple *rtype for the same
// type are coalesced into a single Type.
func toType(t *rtype) Type {
	if t == nil {
		return nil
	}
	return t
}

// emptyInterface is the header for an interface{} value.
type emptyInterface struct {
	typ  *rtype
	word unsafe.Pointer
}

// nonEmptyInterface is the header for an interface value with methods.
type nonEmptyInterface struct {
	// see ../runtime/iface.go:/Itab
	itab *struct {
		ityp *rtype // static interface type
		typ  *rtype // dynamic concrete type
		hash uint32 // copy of typ.hash
		_    [4]byte
		fun  [100000]unsafe.Pointer // method table
	}
	word unsafe.Pointer
}

//再看看rtype
// rtype is the common implementation of most values.
// It is embedded in other struct types.
//
// rtype must be kept in sync with ../runtime/type.go:/^type._type.
//可以发现emptyInterface和nonEmptyInterface中的rtype是对应的eface和iface中的_type的，并且这两者还会保持同步一致的关系
type rtype struct {
	size       uintptr
	ptrdata    uintptr  // number of bytes in the type that can contain pointers
	hash       uint32   // hash of type; avoids computation in hash tables
	tflag      tflag    // extra type information flags
	align      uint8    // alignment of variable with this type
	fieldAlign uint8    // alignment of struct field with this type
	kind       uint8    // enumeration for C
	alg        *typeAlg // algorithm table
	gcdata     *byte    // garbage collection data
	str        nameOff  // string form
	ptrToThis  typeOff  // type for pointer to this type, may be zero
}
```

上面这里可以说明反射是如何将interface中的type转化为reflect中的type的

下面看看reflect.ValueOf()方法

```
// ValueOf returns a new Value initialized to the concrete value
// stored in the interface i. ValueOf(nil) returns the zero Value.
func ValueOf(i interface{}) Value {
	if i == nil {
		return Value{}
	}

	// TODO: Maybe allow contents of a Value to live on the stack.
	// For now we make the contents always escape to the heap. It
	// makes life easier in a few places (see chanrecv/mapassign
	// comment below).
	escapes(i)

	return unpackEface(i)
}

// Dummy annotation marking that the value x escapes,
// for use in cases where the reflect code is so clever that
// the compiler cannot follow.
func escapes(x interface{}) {
	if dummy.b {
		dummy.x = x
	}
}

var dummy struct {
	b bool
	x interface{}
}

//这里同TypeOf方法类似，将interface转成emptyInterface,最终是获取的emptyInterface中的data
// unpackEface converts the empty interface i to a Value.
func unpackEface(i interface{}) Value {
	e := (*emptyInterface)(unsafe.Pointer(&i))
	// NOTE: don't read e.word until we know whether it is really a pointer or not.
	t := e.typ
	if t == nil {
		return Value{}
	}
	f := flag(t.Kind())
	if ifaceIndir(t) {
		f |= flagIndir
	}
	return Value{t, e.word, f}
}

```

所以我们可以得出第一法则，就是通过反射，能够将interface结构转化成反射中的emptyInterface，然后获取的是反射中的Type和Value对象

从unpack方法，也可以得出第三法则，golang的方法调用是值传递，也就是传递的原始数据的一个副本，在unpack方法中，得到的是副本值构造出来的一个Value，所以执行value.Set()方法是不会影响到原始数据的，必须要在执行reflec.ValueOf的时候传递原始数据的指针才行。



第二法则，当然了，反射对象也能够转化成接口对象，但是要转换成原始对象的话，还是要经过显示的强制转换的。