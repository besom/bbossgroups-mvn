/* ========================================================================
 *
 *  This file is part of CODEC, which is a Java package for encoding
 *  and decoding ASN.1 data structures.
 *
 *  Author: Fraunhofer Institute for Computer Graphics Research IGD
 *          Department A8: Security Technology
 *          Fraunhoferstr. 5, 64283 Darmstadt, Germany
 *
 *  Rights: Copyright (c) 2004 by Fraunhofer-Gesellschaft 
 *          zur Foerderung der angewandten Forschung e.V.
 *          Hansastr. 27c, 80686 Munich, Germany.
 *
 * ------------------------------------------------------------------------
 *
 *  The software package is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of the 
 *  License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public 
 *  License along with this software package; if not, write to the Free 
 *  Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 *  MA 02110-1301, USA or obtain a copy of the license at 
 *  http://www.fsf.org/licensing/licenses/lgpl.txt.
 *
 * ------------------------------------------------------------------------
 *
 *  The CODEC library can solely be used and distributed according to 
 *  the terms and conditions of the GNU Lesser General Public License for 
 *  non-commercial research purposes and shall not be embedded in any 
 *  products or services of any user or of any third party and shall not 
 *  be linked with any products or services of any user or of any third 
 *  party that will be commercially exploited.
 *
 *  The CODEC library has not been tested for the use or application 
 *  for a determined purpose. It is a developing version that can 
 *  possibly contain errors. Therefore, Fraunhofer-Gesellschaft zur 
 *  Foerderung der angewandten Forschung e.V. does not warrant that the 
 *  operation of the CODEC library will be uninterrupted or error-free. 
 *  Neither does Fraunhofer-Gesellschaft zur Foerderung der angewandten 
 *  Forschung e.V. warrant that the CODEC library will operate and 
 *  interact in an uninterrupted or error-free way together with the 
 *  computer program libraries of third parties which the CODEC library 
 *  accesses and which are distributed together with the CODEC library.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not warrant that the operation of the third parties's computer 
 *  program libraries themselves which the CODEC library accesses will 
 *  be uninterrupted or error-free.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  shall not be liable for any errors or direct, indirect, special, 
 *  incidental or consequential damages, including lost profits resulting 
 *  from the combination of the CODEC library with software of any user 
 *  or of any third party or resulting from the implementation of the 
 *  CODEC library in any products, systems or services of any user or 
 *  of any third party.
 *
 *  Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. 
 *  does not provide any warranty nor any liability that utilization of 
 *  the CODEC library will not interfere with third party intellectual 
 *  property rights or with any other protected third party rights or will 
 *  cause damage to third parties. Fraunhofer Gesellschaft zur Foerderung 
 *  der angewandten Forschung e.V. is currently not aware of any such 
 *  rights.
 *
 *  The CODEC library is supplied without any accompanying services.
 *
 * ========================================================================
 */
package codec.pkcs7;

import java.math.BigInteger;
import java.security.Principal;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import codec.asn1.ASN1Integer;
import codec.asn1.ASN1ObjectIdentifier;
import codec.asn1.ASN1Opaque;
import codec.asn1.ASN1RegisteredType;
import codec.asn1.ASN1Sequence;
import codec.asn1.ASN1Set;
import codec.asn1.ASN1SetOf;
import codec.asn1.ASN1TaggedType;
import codec.asn1.ASN1Type;
import codec.util.CertificateSource;
import codec.x509.AlgorithmIdentifier;

/**
 * The signatures generated by this class are compatible to Sun's
 * <code>jarsigner</code>. The actual bytes being signed are denoted
 * <i>payload</i> in this documenatation, in order to differentiate between the
 * signing of arbitrary (opaque) data and the DER encoding of registered ASN.1
 * structures such as EnvelopedData.
 * <p>
 * 
 * Presently, only content of type {@link Data Data} is supported. Either
 * detached signatures may be generated (in which case the content consists of a
 * {@link Data Data} type with no content) or the payload may be embedded into
 * the content info of this structure (automatically wrapped into a
 * {@link Data Data} type.
 * <p>
 * 
 * Use {@link SignerInfo SignerInfo} instances for signing and verifying
 * instances of this class such as illustrated in the code example below. This
 * example shows how to verify a detached signature on a file. One PKCS#7
 * structure may contain multiple signatures. In the example given below, all of
 * them are verified. <blockquote>
 * 
 * <pre>
 * public void verifyFile(SignedData sd, File file) {
 *     boolean ok;
 *     Iterator i;
 *     Verifier verifier;
 *     SignerInfo info;
 *     FileInputStream in;
 *     for (i = sd.getSignerInfos().iterator(); i.hasNext();) {
 * 	info = (SignerInfo) i.next();
 * 	System.out.println(&quot;\nVerifying:\n&quot; + info.toString());
 * 	verifier = new Verifier(sd, info, null);
 * 	in = new FileInputStream(file);
 * 	verifier.update(in);
 * 	in.close();
 * 	ok = (verifier.verify() != null);
 * 
 * 	System.out.println(ok ? &quot;Signature OK&quot; : &quot;BAD SIGNATURE!&quot;);
 *     }
 * }
 * </pre>
 * 
 * </blockquote> If the data embedded in a SignedData instance shall be verified
 * then this data must be retrieved by means of the {@link #getData getData}
 * method first and must be passed to one of the update methods just as the
 * detached data in the example above.
 * <p>
 * 
 * Likewise, if data shall be signed and attached to a SignedData instance then
 * the signing process of that data must be completed as for detached data. The
 * signed data then can be attached to the SignedData instance by means of the
 * {@link #setData setData} method.
 * 
 * The definition of this structure is: <blockquote>
 * 
 * <pre>
 * SignedData ::= SEQUENCE {
 *   version Version,
 *   digestAlgorithms DigestAlgorithmIdentifiers,
 *   contentInfo ContentInfo,
 *   certificates
 *     [0] IMPLICIT ExtendedCertificatesAndCertificates OPTIONAL,
 *   crls
 *     [1] IMPLICIT CertificateRevocationLists OPTIONAL,
 *   signerInfos SignerInfos
 * }
 * DigestAlgorithmIdentifiers ::= SET OF DigestAlgorithmIdentifier
 * SignerInfos ::= SET OF SignerInfo
 * </pre>
 * 
 * </blockquote>
 * 
 * Please note that <code>SignerInfo</code> structures only store the issuer
 * and serial number of the signing certificate but not the certificate itself.
 * Neither are certificates added automatically by this class when signing is
 * done. If a certificate shall be included with an instance of this class then
 * it must be added explicitly by calling <code>addCertificate(..)</code>.
 * 
 * @author Volker Roth
 * @version "$Id: SignedData.java,v 1.8 2004/08/12 12:25:19 pebinger Exp $"
 */
public class SignedData extends ASN1Sequence implements ASN1RegisteredType,
	CertificateSource, Signable {
    /**
     * The OID of this structure. PKCS#7 SignedData.
     */
    private static final int[] THIS_OID = { 1, 2, 840, 113549, 1, 7, 2 };

    /**
     * The PKCS#7 Data OID.
     */
    private static final int[] DATA_OID = { 1, 2, 840, 113549, 1, 7, 1 };

    /**
     * The DigestAlgorithmIdentifiers.
     */
    protected ASN1Set digestID_;

    /**
     * The X.509 certificates.
     */
    protected Certificates certs_;

    /**
     * The {@link SignerInfo SignerInfos}.
     */
    protected ASN1SetOf infos_;

    /**
     * The revocation lists.
     */
    protected ASN1Set crls_;

    /**
     * The {@link ContentInfo ContentInfo}.
     */
    protected ContentInfo content_;

    /**
     * The cache encoded X.509 certificates. This cache is filled with opaque
     * versions on encoding this instance.
     */
    protected ASN1Set cache_;

    /**
     * The certificate factory that is used for decoding certificates.
     */
    protected CertificateFactory factory_;

    /**
     * Creates an instance ready for decoding.
     */
    public SignedData() {
	super(6);

	add(new ASN1Integer(1)); // version

	digestID_ = new ASN1SetOf(AlgorithmIdentifier.class);
	add(digestID_);

	content_ = new ContentInfo();
	add(content_);

	certs_ = new Certificates();
	add(new ASN1TaggedType(0, certs_, false, true));

	crls_ = new ASN1SetOf(ASN1Opaque.class);
	add(new ASN1TaggedType(1, crls_, false, true));

	infos_ = new ASN1SetOf(SignerInfo.class);
	add(infos_);
    }

    /**
     * Creates an instance ready for decoding, allows the setting of the
     * Version. needed for instance for TSP Structures.
     */
    public SignedData(int _version) {
	super(6);

	add(new ASN1Integer(_version)); // version

	digestID_ = new ASN1SetOf(AlgorithmIdentifier.class);
	add(digestID_);

	content_ = new ContentInfo();
	add(content_);

	certs_ = new Certificates();
	add(new ASN1TaggedType(0, certs_, false, true));

	crls_ = new ASN1SetOf(ASN1Opaque.class);
	add(new ASN1TaggedType(1, crls_, false, true));

	infos_ = new ASN1SetOf(SignerInfo.class);
	add(infos_);
    }

    /**
     * This method retrieves the content of this structure, consisting of the
     * ASN.1 type embedded in the {@link #content_ ContentInfo} structure.
     * Beware, the content type might be faked by adversaries, if it is not of
     * type {@link Data Data}. If it is not data then the authenticated content
     * type must be given as an authenticated attribute in all the
     * {@link SignerInfo SignerInfo} structures.
     * 
     * @return The contents octets.
     */
    public ASN1Type getContent() {
	return content_.getContent();
    }

    /**
     * Sets the content type to {@link Data Data} and clears the actual content.
     * Call this method when external data is signed, and no particular content
     * type shall be used. This method calls <code>
     * setContentType(new ASN1ObjectIdentifier(DATA_OID))
     * </code>.
     */
    public void setDataContentType() {
	setContentType(new ASN1ObjectIdentifier(DATA_OID));
    }

    /**
     * Sets the content type to the given OID. The content itself is set to
     * <code>null</code>. This method should be called if the content to be
     * signed is external (not inserted into this structure).
     * <p>
     * 
     * If this structure is signed with the {@link Signer Signer} then the
     * {@link SignerInfo SignerInfo} that is passed to it must have either:
     * <ul>
     * <li> no authenticated content type attribute, or
     * <li> the authenticated content type attribute must match <code>oid</code>.
     * </ul>
     * In the first case, a new authenticated content type attribute with
     * <code>oid</code> as its value will be added to the
     * <code>SignerInfo</code> automatically (if the content type is not
     * {@link Data Data} or at least one other authenticated attribute is
     * already in that <code>SignerInfo</code>.
     * 
     * @param oid
     *                The OID that identifies the content type of the signed
     *                data.
     * @throws NullPointerException
     *                 if <code>oid</code> is <code>null</code>.
     */
    public void setContentType(ASN1ObjectIdentifier oid) {
	if (oid == null) {
	    throw new NullPointerException("OID");
	}
	content_.setContent(oid);
    }

    /**
     * Sets the content to be embedded into this instance's
     * <code>ContentInfo</code>.
     * 
     * @param t
     *                The actual content.
     */
    public void setContent(ASN1RegisteredType t) {
	if (t == null) {
	    throw new NullPointerException("Need content!");
	}
	content_.setContent(t);
    }

    /**
     * Sets the content to be embedded into this instance's
     * <code>ContentInfo</code>.
     * 
     * @param oid
     *                The object identifier of the content.
     * @param t
     *                The actual content.
     */
    public void setContent(ASN1ObjectIdentifier oid, ASN1Type t) {
	if (oid == null || t == null) {
	    throw new NullPointerException("Need an OID and content!");
	}
	content_.setContent(oid, t);
    }

    /**
     * Returns the content type of the content embedded in this structure. The
     * returned OID is a copy, no side effects are caused by modifying it.
     * 
     * @return The content type of this structure's payload.
     */
    public ASN1ObjectIdentifier getContentType() {
	return (ASN1ObjectIdentifier) content_.getContentType().clone();
    }

    /**
     * This method wraps the given bytes into a {@link Data Data} type and sets
     * it as the content.
     * <p>
     * 
     * Please note that the signing process implemented in this class does not
     * care about the content. Setting a content before signing does <b>not</b>
     * sign the content. The data to be signed must always be passed to one of
     * the <code>
     * update</code> methods.
     * 
     * @param b
     *                The opaque contents to embed in this structure.
     */
    public void setData(byte[] b) {
	content_.setContent(new Data(b));
    }

    /**
     * This method retrieves the content from this structure's
     * {@link ContentInfo ContentInfo} structure. In general, this will be of
     * type {@link Data Data}. The actual content type can be retrieved by
     * calling {@link #getContentType getContentType}. If the type is Data,
     * then {@link #getData getData} might be called. If the content type is
     * Data then the easiest way to retrieve the actual payload bytes is to
     * call:
     * <p>
     * <code>
     * signedData.{@link #getData getData}().{@link
     *    Data#getByteArray() getByteArray}()
     * </code>
     * 
     * @throws NoSuchElementException
     *                 if the content type is not {@link Data Data}.
     */
    public Data getData() throws NoSuchElementException {
	ASN1Type o;

	o = content_.getContent();

	if (o == null) {
	    return null;
	}
	if (o instanceof Data) {
	    return (Data) o;
	}
	throw new NoSuchElementException("Content type is not Data!");
    }

    /**
     * This method returns <code>true</code> if this structure has content of
     * type {@link Data Data} and the content contained in it is not null.
     * 
     * @return <code>true</code> if there is a payload.
     */
    public boolean hasData() {
	ASN1Type o;
	byte[] b;

	o = content_.getContent();

	if (o == null) {
	    return false;
	}
	if (!(o instanceof Data)) {
	    return false;
	}
	b = ((Data) o).getByteArray();

	if (b == null || b.length == 0) {
	    return false;
	}
	return true;
    }

    /**
     * Returns the OID of this structure. The returned OID is a copy, no side
     * effects are caused by modifying it.
     * 
     * @return The OID.
     */
    public ASN1ObjectIdentifier getOID() {
	return new ASN1ObjectIdentifier(THIS_OID);
    }

    /**
     * Sets the certificate factory to use for decoding certificates.
     * 
     * @param factory
     *                The certificate factory or <code>null
     *   </code> if the
     *                default <code>X.509</code> factory shall be used.
     */
    public void setCertificateFactory(CertificateFactory factory) {
	certs_.setCertificateFactory(factory);
    }

    /**
     * This method returns the certificates stored in this structure. Each
     * certificate can be casted to a <code>X509Certificate</code>.
     * 
     * @return An unmodifiable list view of the certificates.
     */
    public List getCertificates() {
	return Collections.unmodifiableList(certs_);
    }

    /**
     * Adds the given certificate to this structure if none with the same issuer
     * and serial number already exists.
     * 
     * @param cert
     *                The certificate to add.
     */
    public void addCertificate(X509Certificate cert) {
	if (certs_.addCertificate(cert)) {
	    ((ASN1Type) get(3)).setOptional(false);
	}
    }

    public X509Certificate getCertificate(Principal issuer, BigInteger serial) {
	return certs_.getCertificate(issuer, serial);
    }

    public Iterator certificates(Principal subject) {
	return certs_.certificates(subject);
    }

    public Iterator certificates(Principal subject, int keyUsage) {
	return certs_.certificates(subject, keyUsage);
    }

    /**
     * This method returns the {@link SignerInfo SignerInfos} of the signers of
     * this structure.
     * 
     * @return The unmodifiable view of the list of SignerInfos.
     */
    public List getSignerInfos() {
	return Collections.unmodifiableList(infos_);
    }

    /**
     * Returns the <code>SignerInfo</code> that matches the given certificate.
     * 
     * @param cert
     *                The certificate matching the <code>SignerInfo
     *   </code> to
     *                be retrieved.
     * @return The <code>SignerInfo</code> or <code>null</code> if no
     *         matching one is found.
     */
    public SignerInfo getSignerInfo(X509Certificate cert) {
	SignerInfo info;
	Iterator i;

	for (i = getSignerInfos().iterator(); i.hasNext();) {
	    info = (SignerInfo) i.next();

	    if (!info.getIssuerDN().equals(cert.getIssuerDN())) {
		continue;
	    }
	    if (info.getSerialNumber().equals(cert.getSerialNumber())) {
		return info;
	    }
	}
	return null;
    }

    /**
     * Returns a string representation of this object.
     * 
     * @return The string representation.
     */
    public String toString() {
	return "-- PKCS#7 SignedData --\n" + super.toString();
    }

    /**
     * Adds the given {@link SignerInfo SignerInfo} to this instance. This
     * method should be used rarely. In general, the signing methods take care
     * of adding <code>SignerInfo
     * </code> instances. Explicit adding of a
     * <code>SignerInfo
     * </code> is provided only in those cases where fine
     * control of the creation of signatures is required.
     * 
     * @param info
     *                The <code>SignerInfo</code> to add.
     * @throws NullPointerException
     *                 if the <code>info</code> is <code>null</code>.
     */
    public void addSignerInfo(SignerInfo info) {
	AlgorithmIdentifier idn;
	AlgorithmIdentifier idv;
	Iterator i;

	if (info == null) {
	    throw new NullPointerException("Need a SignerInfo!");
	}
	infos_.add(info);

	/*
	 * We also have to add the DigestAlgorithmIdentifier of the SignerInfo
	 * to the list of digest algs if it is not yet in the list.
	 */
	idn = info.getDigestAlgorithmIdentifier();

	for (i = digestID_.iterator(); i.hasNext();) {
	    idv = (AlgorithmIdentifier) i.next();

	    if (idn.equals(idv)) {
		return;
	    }
	}
	digestID_.add(idn);
    }

}
