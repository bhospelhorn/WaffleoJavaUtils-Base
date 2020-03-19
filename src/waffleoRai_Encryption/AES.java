package waffleoRai_Encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {
	
	//Yeah, I don't feel like debugging it.
	//Let's see what the Java libraries have...
	//https://www.novixys.com/blog/java-aes-example/
	
	/* ----- Constants ----- */
	
	//public static final String CIPHER_TRANSFORMATION_CBC = "AES/CBC/NoPadding";
	//public static final String CIPHER_TRANSFORMATION_CBC_PADDING = "AES/CBC/PKCS5Padding";
	
	/* ----- Static Variables ----- */
	
	/* ----- Instance Variables ----- */
	
	private byte[] aes_key;
	private String cipher_str;
	private String padding;
	
	private Cipher cipher;
	private SecretKeySpec skey;
	private IvParameterSpec ivspec;
	
	/* ----- Construction ----- */
	
	public AES(byte[] key)
	{
		aes_key = key;
		cipher_str = "CBC";
	}
	
	/**
	 * Construct an AES encrytor/decryptor with the provided AES key
	 * as an array of bytes 0-extended to ints.
	 * <br><b>!IMPORTANT!</b> The upper 24 bits of every int provided are IGNORED!
	 * The reason this overload exists is because Java refuses to perform bit level
	 * operations on any primitive smaller than an int.
	 * @param key AES key as an array of bytes 0-extended to ints.
	 */
	public AES(int[] key)
	{
		aes_key = new byte[16];
		for(int i = 0; i < key.length; i++) aes_key[i] = (byte)key[i];
		cipher_str = "CBC";
	}
		
	/* ----- Getters ----- */
	
	public String getCipherString()
	{
		String padstr = "NoPadding";
		if(padding != null) padstr = padding;
		return "AES/" + cipher_str + "/" + padstr;
	}
	
	public boolean paddingSet()
	{
		return (padding != null);
	}
	
	/* ----- Setters ----- */
	
	public void setKey(byte[] newkey)
	{
		aes_key = newkey;
	}
	
	public void setCBC()
	{
		cipher_str = "CBC";
	}
	
	public void setCTR()
	{
		cipher_str = "CTR";
	}
	
	public void setCCM()
	{
		cipher_str = "CCM";
	}
	
	public void setPadding(boolean b)
	{
		if(b) padding = "PKCS5Padding";
		else padding = null;
	}
	
	public void reset()
	{
		cipher = null;
		skey = null;
		ivspec = null;
	}
	
	/* ----- Decryption ----- */
	
	public byte[] decrypt(byte[] iv, byte[] in)
	{
		try 
		{
			Cipher cipher = Cipher.getInstance(getCipherString());
			SecretKeySpec skey = new SecretKeySpec(aes_key, "AES");
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, skey, ivspec);
			return cipher.doFinal(in);
		} 
		catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (NoSuchPaddingException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (InvalidKeyException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (IllegalBlockSizeException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (BadPaddingException e) 
		{
			e.printStackTrace();
			return null;
		}
	}

	public boolean initDecrypt(byte[] iv)
	{
		try 
		{
			cipher = Cipher.getInstance(getCipherString());
			skey = new SecretKeySpec(aes_key, "AES");
			ivspec = new IvParameterSpec(iv);
			
			cipher.init(Cipher.DECRYPT_MODE, skey, ivspec);
		} 
		catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
			return false;
		} 
		catch (InvalidKeyException e) 
		{
			e.printStackTrace();
			return false;
		} 
		catch (InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public byte[] decryptBlock(byte[] in, boolean finalBlock)
	{
		if(cipher == null) return null;
		
		if(finalBlock)
		{
			try {return cipher.doFinal(in);} 
			catch (IllegalBlockSizeException e) {e.printStackTrace(); return null;} 
			catch (BadPaddingException e) {e.printStackTrace(); return null;} 
		}
		return cipher.update(in);
		
	}
	
	/* ----- Encryption ----- */
	
	public byte[] encrypt(byte[] iv, byte[] in)
	{
		try 
		{
			Cipher cipher = Cipher.getInstance(getCipherString());
			SecretKeySpec skey = new SecretKeySpec(aes_key, "AES");
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			cipher.init(Cipher.ENCRYPT_MODE, skey, ivspec);
			return cipher.doFinal(in);
		} 
		catch (NoSuchAlgorithmException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (NoSuchPaddingException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (InvalidKeyException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (InvalidAlgorithmParameterException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (IllegalBlockSizeException e) 
		{
			e.printStackTrace();
			return null;
		} 
		catch (BadPaddingException e) 
		{
			e.printStackTrace();
			return null;
		}
	}
	
}
