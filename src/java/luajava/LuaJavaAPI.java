/*
* Copyright (C) 2003 Thiago Ponte, Rafael Rizzato.  All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining
* a copy of this software and associated documentation files (the
* "Software"), to deal in the Software without restriction, including
* without limitation the rights to use, copy, modify, merge, publish,
* distribute, sublicense, and/or sell copies of the Software, and to
* permit persons to whom the Software is furnished to do so, subject to
* the following conditions:
*
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package luajava;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Class that contains functions accessed by lua.
 * @author Thiago
 */
public final class LuaJavaAPI
{

	private LuaJavaAPI()
	{
	}

	/**
	 * Java implementation of the metamethod __index
	 * @param luaState
	 * @param obj
	 * @param method
	 * @return int
	 */
	public static int objectIndex(int luaState, Object obj, String methodName)
		throws Exception
	{
		LuaState L =
			LuaStateFactory.getExistingState(luaState);

		int top = L.getTop();

		Object[] objs = new Object[top - 1];

		Class clazz;

		if (obj instanceof Class)
		{
			clazz = (Class) obj;
		}
		else
		{
			clazz = obj.getClass();
		}

		Method[] methods = clazz.getMethods();
		Method method = null;

		// gets method and arguments
		for (int i = 0; i < methods.length; i++)
		{
			if (!methods[i].getName().equals(methodName))
			
				continue;

			Class[] parameters = methods[i].getParameterTypes();
			if (parameters.length != top - 1)
				continue;
			
			boolean okMethod = true;

			for (int j = 0; j < parameters.length; j++)
			{
				try
				{
					objs[j] = compareTypes( L , parameters[j] , j+2 );
				}
				catch (Exception e)
				{
					okMethod = false;
					break;
				}
			}

			if (okMethod)
			{
				method = methods[i];
				break;
			}

		}

		// If method is null means there isn't one receiving the given arguments
		if (method == null)
		{
			throw new LuaException("Invalid method call . No such method .");
		}

		Object ret;
		try
		{
			if (obj instanceof Class)
			{
				ret = method.invoke(null, objs);
			}
			else
			{
				ret = method.invoke(obj, objs);
			}
		}
		catch (Exception e)
		{
			throw new LuaException(e);
		}

		// Void function returns null
		if (ret == null)
		{
			return 0;
		}

		// push result
		L.pushObjectValue( ret );
		
		return 1;
	}

	/**
	 * Java function to be called when a java Class metamethod __index
	 * is called.
	 * This function returns 1 if there is a field with searchName and 2 
	 * if there is a method if the searchName
	 * @param luaState
	 * @param clazz
	 * @param fieldName
	 * @return int
	 * @throws Exception
	 */
	public static int classIndex(int luaState, Class clazz, String searchName)
		throws Exception
	{
		int res;

		res = checkField(luaState, clazz, searchName);

		if (res != 0)
		{
			return 1;
		}

		res = checkMethod(luaState, clazz, searchName);

		if (res != 0)
		{
			return 2;
		}

		return 0;
	}

	/**
	 * Pushes a new instance of a java Object of the type className
	 * @param luaState
	 * @param className
	 * @return int
	 * @throws Exception
	 */
	public static int javaNewInstance(int luaState, String className)
		throws Exception
	{
		LuaState L =
			LuaStateFactory.getExistingState(luaState);

		Class clazz = Class.forName(className);

		Object ret = getObjInstance( L , clazz );

		L.pushJavaObject(ret);

		return 1;
	}
	
	/**
	 * javaNew returns a new instance of a given clazz
	 * @param luaState
	 * @param clazz
	 * @return int
	 * @throws LuaException
	 */
	public static int javaNew( int luaState , Class clazz )
		throws LuaException
	{
		LuaState L = LuaStateFactory.getExistingState( luaState );
		
		Object ret = getObjInstance( L , clazz );

		L.pushJavaObject(ret);

		return 1;
	}
	
	
	private static Object getObjInstance( LuaState L , Class clazz )
		throws LuaException
	{
		int top = L.getTop();

		Object[] objs = new Object[top-1];

		Constructor[] constructors = clazz.getConstructors();
		Constructor constructor = null;

		// gets method and arguments
		for (int i = 0; i < constructors.length; i++)
		{
			Class[] parameters = constructors[i].getParameterTypes();
			if (parameters.length != top-1)
				continue;

			boolean okConstruc = true;

			for (int j = 0; j < parameters.length; j++)
			{
				try
				{
					objs[j] = compareTypes( L , parameters[j] , j+2 );
				}
				catch (Exception e)
				{
					okConstruc = false;
					break;
				}
				
			}

			if (okConstruc)
			{
				constructor = constructors[i];
				break;
			}

		}

		// If method is null means there isn't one receiving the given arguments
		if (constructor == null)
		{
			throw new LuaException("Invalid method call . No such method .");
		}

		Object ret;
		try
		{
			ret = constructor.newInstance(objs);
		}
		catch (Exception e)
		{
			throw new LuaException(e);
		}

		if (ret == null)
		{
			throw new LuaException("Couldn't instantiate java Object");
		}

		return ret;
	}

	/**
	 * Checks if there is a field on the obj with the given name
	 * @param luaState
	 * @param obj
	 * @param fieldName
	 * @return int
	 */
	public static int checkField(int luaState, Object obj, String fieldName)
	{
		Field field = null;
		Class objClass;

		if (obj instanceof Class)
		{
			objClass = (Class) obj;
		}
		else
		{
			objClass = obj.getClass();
		}

		try
		{
			field = objClass.getField(fieldName);
		}
		catch (Exception e)
		{
			return 0;
		}

		if (field == null)
		{
			return 0;
		}

		Object ret = null;
		try
		{
			ret = field.get(obj);
		}
		catch (Exception e1)
		{
			return 0;
		}

		if (obj == null)
		{
			return 0;
		}

		LuaState L =
			LuaStateFactory.getExistingState(luaState);

		L.pushObjectValue( ret );

		return 1;
	}

	/**
	 * Checks to see if there is a method with the given name.
	 * @param luaState
	 * @param obj
	 * @param fieldName
	 * @return int
	 */
	public static int checkMethod(int luaState, Object obj, String fieldName)
	{
		LuaState L =
			LuaStateFactory.getExistingState(luaState);

		Class clazz;

		int top = L.getTop();

		if (obj instanceof Class)
		{
			clazz = (Class) obj;
		}
		else
		{
			clazz = obj.getClass();
		}

		Method[] methods = clazz.getMethods();

		for (int i = 0; i < methods.length; i++)
		{
			if (methods[i].getName().equals(fieldName))
				return 1;
		}

		return 0;
	}

	/**
	 * Function that creates an object proxy and pushes it into the stack
	 * @param luaState
	 * @param implem
	 * @return int
	 * @throws LuaException
	 */
	public static int createProxyObject(int luaState, String implem) throws LuaException
	{
		try
		{
			LuaState L =
				LuaStateFactory.getExistingState(luaState);

			if (!(L.isTable(2)))
				throw new LuaException("Parameter is not a table. Can't create proxy.");

			LuaObject luaObj = L.getLuaObject(2);

			Object proxy = luaObj.createProxy(implem);
			L.pushJavaObject(proxy);
		}
		catch (Exception e)
		{
			throw new LuaException(e);
		}

		return 1;
	}
	
	private static Object compareTypes( LuaState L , Class parameter , int idx ) throws Exception
	{
		boolean okType = true;
		Object obj = null;
		
		if (L.isBoolean(idx))
		{
			if (parameter.isPrimitive())
			{
				if (parameter != Boolean.TYPE)
				{
					okType = false;
				}
			}
			else if (!parameter.isAssignableFrom(Boolean.class))
			{
				okType = false;
			}
			obj = new Boolean(L.toBoolean(idx));
		}
		else if (L.type(idx) == LuaState.LUA_TSTRING.intValue())
		{
			if (!parameter.isAssignableFrom(String.class))
			{
				okType = false;
			}
			obj = L.toString(idx);
		}
		else if (L.isFunction(idx))
		{
			if (!parameter.isAssignableFrom(LuaObject.class))
			{
				okType = false;
			}
			obj = L.getLuaObject(idx);
		}
		else if (L.isTable(idx))
		{
			if (!parameter.isAssignableFrom(LuaObject.class))
			{
				okType = false;
			}
			obj = L.getLuaObject(idx);
		}
		else if (L.type(idx) == LuaState.LUA_TNUMBER.intValue())
		{
			if (parameter.isPrimitive())
			{
				Double db = new Double(L.toNumber(idx));

				if (parameter == Integer.TYPE)
				{
					obj = new Integer(db.intValue());
				}
				else if (parameter == Long.TYPE)
				{
					obj = new Long(db.longValue());
				}
				else if (parameter == Float.TYPE)
				{
					obj = new Float(db.floatValue());
				}
				else if (parameter == Double.TYPE)
				{
					obj = db;
				}
				else if (parameter == Byte.TYPE)
				{
					obj = new Long(db.byteValue());
				}
				else if (parameter == Short.TYPE)
				{
					obj = new Long(db.shortValue());
				}
			}

			else if (!parameter.isAssignableFrom(Number.class))
			{
				okType = false;
			}

			Double db = new Double(L.toNumber(idx));

			// Checks all possibilities of native types
			if (parameter.isAssignableFrom(Integer.class))
			{
				obj = new Integer(db.intValue());
			}
			else if (parameter.isAssignableFrom(Long.class))
			{
				obj = new Long(db.longValue());
			}
			else if (parameter.isAssignableFrom(Float.class))
			{
				obj = new Float(db.floatValue());
			}
			else if (parameter.isAssignableFrom(Double.class))
			{
				obj = db;
			}
			else if (parameter.isAssignableFrom(Byte.class))
			{
				obj = new Byte(db.byteValue());
			}
			else if (parameter.isAssignableFrom(Short.class))
			{
				obj = new Short(db.shortValue());
			}
		}
		else if (L.isUserdata(idx))
		{
			if (L.isObject(idx))
			{
				Object userObj = L.getObjectFromUserdata(idx);
				if (!parameter.isAssignableFrom(userObj.getClass()))
				{
					okType = false;
				}
				obj = userObj;
			}
			else
			{
				if (!parameter.isAssignableFrom(LuaObject.class))
				{
					okType = false;
				}
			}
		}
		else if (L.isNil(idx))
		{
			obj = null;
		}
		else
		{
			throw new LuaException("Invalid Parameters .");
		}
		
		if ( !okType )
		{
			throw new Exception("Invalid Parameter .");
		}
		
		return obj;
	}

}