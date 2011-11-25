/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: E:\\eclipse\\workspace\\VTI\\src\\com\\social\\services\\ISocialService.aidl
 */
package com.social.services;
public interface ISocialService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.social.services.ISocialService
{
private static final java.lang.String DESCRIPTOR = "com.social.services.ISocialService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.social.services.ISocialService interface,
 * generating a proxy if needed.
 */
public static com.social.services.ISocialService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.social.services.ISocialService))) {
return ((com.social.services.ISocialService)iin);
}
return new com.social.services.ISocialService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_getSocialFeed:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<com.social.model.Twit> _result = this.getSocialFeed();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
case TRANSACTION_getCurrentSocialFeed:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<com.social.model.Twit> _result = this.getCurrentSocialFeed();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.social.services.ISocialService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public java.util.List<com.social.model.Twit> getSocialFeed() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<com.social.model.Twit> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSocialFeed, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(com.social.model.Twit.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.util.List<com.social.model.Twit> getCurrentSocialFeed() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<com.social.model.Twit> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCurrentSocialFeed, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(com.social.model.Twit.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getSocialFeed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getCurrentSocialFeed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public java.util.List<com.social.model.Twit> getSocialFeed() throws android.os.RemoteException;
public java.util.List<com.social.model.Twit> getCurrentSocialFeed() throws android.os.RemoteException;
}
