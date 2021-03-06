/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.zkoss.poi.hssf.usermodel;

import org.zkoss.poi.ddf.*;
import org.zkoss.poi.hssf.record.CommonObjectDataSubRecord;
import org.zkoss.poi.hssf.record.ObjRecord;
import org.zkoss.poi.util.LittleEndian;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * An abstract shape.
 *
 * @author Glen Stampoultzis (glens at apache.org)
 */
public abstract class HSSFShape {
    public static final int LINEWIDTH_ONE_PT = 12700;
    public static final int LINEWIDTH_DEFAULT = 9525;
    public static final int LINESTYLE__COLOR_DEFAULT = 0x08000040;
    public static final int FILL__FILLCOLOR_DEFAULT = 0x08000009;
    public static final boolean NO_FILL_DEFAULT = true;

    public static final int LINESTYLE_SOLID = 0;              // Solid (continuous) pen
    public static final int LINESTYLE_DASHSYS = 1;            // PS_DASH system   dash style
    public static final int LINESTYLE_DOTSYS = 2;             // PS_DOT system   dash style
    public static final int LINESTYLE_DASHDOTSYS = 3;         // PS_DASHDOT system dash style
    public static final int LINESTYLE_DASHDOTDOTSYS = 4;      // PS_DASHDOTDOT system dash style
    public static final int LINESTYLE_DOTGEL = 5;             // square dot style
    public static final int LINESTYLE_DASHGEL = 6;            // dash style
    public static final int LINESTYLE_LONGDASHGEL = 7;        // long dash style
    public static final int LINESTYLE_DASHDOTGEL = 8;         // dash short dash
    public static final int LINESTYLE_LONGDASHDOTGEL = 9;     // long dash short dash
    public static final int LINESTYLE_LONGDASHDOTDOTGEL = 10; // long dash short dash short dash
    public static final int LINESTYLE_NONE = -1;

    public static final int LINESTYLE_DEFAULT = LINESTYLE_NONE;

    // TODO - make all these fields private
    private HSSFShape parent;
    HSSFAnchor anchor;
    private HSSFPatriarch _patriarch;

    private final EscherContainerRecord _escherContainer;
    private final ObjRecord _objRecord;
    private final EscherOptRecord _optRecord;
    
    public final static int NO_FILLHITTEST_TRUE = 0x00110000;
    public final static int NO_FILLHITTEST_FALSE = 0x00010000;

    /**
     * creates shapes from existing file
     * @param spContainer
     * @param objRecord
     */
    public HSSFShape(EscherContainerRecord spContainer, ObjRecord objRecord) {
        this._escherContainer = spContainer;
        this._objRecord = objRecord;
        this._optRecord = spContainer.getChildById(EscherOptRecord.RECORD_ID);
        this.anchor = HSSFAnchor.createAnchorFromEscher(spContainer);
    }

    /**
     * Create a new shape with the specified parent and anchor.
     */
    public HSSFShape(HSSFShape parent, HSSFAnchor anchor) {
        this.parent = parent;
        this.anchor = anchor;
        this._escherContainer = createSpContainer();
        _optRecord = _escherContainer.getChildById(EscherOptRecord.RECORD_ID);
        _objRecord = createObjRecord();

    }

    protected abstract EscherContainerRecord createSpContainer();

    protected abstract ObjRecord createObjRecord();

    /**
     * remove escher container from the patriarch.escherAggregate
     * remove obj, textObj and note records if it's necessary
     * in case of ShapeGroup remove all contained shapes
     * @param patriarch
     */
    protected abstract void afterRemove(HSSFPatriarch patriarch);

    /**
     * @param shapeId - global shapeId which must be set to EscherSpRecord
     */
    void setShapeId(int shapeId){
        EscherSpRecord spRecord = _escherContainer.getChildById(EscherSpRecord.RECORD_ID);
        spRecord.setShapeId(shapeId);
        CommonObjectDataSubRecord cod = (CommonObjectDataSubRecord) _objRecord.getSubRecords().get(0);
        cod.setObjectId((short) (shapeId%1024));
    }

    /**
     * @return global shapeId(from EscherSpRecord)
     */
    int getShapeId(){
        return ((EscherSpRecord)_escherContainer.getChildById(EscherSpRecord.RECORD_ID)).getShapeId();
    }

    abstract void afterInsert(HSSFPatriarch patriarch);

    protected EscherContainerRecord getEscherContainer() {
        return _escherContainer;
    }

    protected ObjRecord getObjRecord() {
        return _objRecord;
    }

    protected EscherOptRecord getOptRecord() {
        return _optRecord;
    }

    /**
     * Gets the parent shape.
     */
    public HSSFShape getParent() {
        return parent;
    }

    /**
     * @return the anchor that is used by this shape.
     */
    public HSSFAnchor getAnchor() {
        return anchor;
    }

    /**
     * Sets a particular anchor.  A top-level shape must have an anchor of
     * HSSFClientAnchor.  A child anchor must have an anchor of HSSFChildAnchor
     *
     * @param anchor the anchor to use.
     * @throws IllegalArgumentException when the wrong anchor is used for
     *                                  this particular shape.
     * @see HSSFChildAnchor
     * @see HSSFClientAnchor
     */
    public void setAnchor(HSSFAnchor anchor) {
        int i = 0;
        int recordId = -1;
        if (parent == null) {
            if (anchor instanceof HSSFChildAnchor)
                throw new IllegalArgumentException("Must use client anchors for shapes directly attached to sheet.");
            EscherClientAnchorRecord anch = _escherContainer.getChildById(EscherClientAnchorRecord.RECORD_ID);
            if (null != anch) {
                for (i=0; i< _escherContainer.getChildRecords().size(); i++){
                    if (_escherContainer.getChild(i).getRecordId() == EscherClientAnchorRecord.RECORD_ID){
                        if (i != _escherContainer.getChildRecords().size() -1){
                            recordId = _escherContainer.getChild(i+1).getRecordId();
                        }
                    }
                }
                _escherContainer.removeChildRecord(anch);
            }
        } else {
            if (anchor instanceof HSSFClientAnchor)
                throw new IllegalArgumentException("Must use child anchors for shapes attached to groups.");
            EscherChildAnchorRecord anch = _escherContainer.getChildById(EscherChildAnchorRecord.RECORD_ID);
            if (null != anch) {
                for (i=0; i< _escherContainer.getChildRecords().size(); i++){
                    if (_escherContainer.getChild(i).getRecordId() == EscherChildAnchorRecord.RECORD_ID){
                        if (i != _escherContainer.getChildRecords().size() -1){
                            recordId = _escherContainer.getChild(i+1).getRecordId();
                        }
                    }
                }
                _escherContainer.removeChildRecord(anch);
            }
        }
        if (-1 == recordId){
            _escherContainer.addChildRecord(anchor.getEscherAnchor());
        } else {
            _escherContainer.addChildBefore(anchor.getEscherAnchor(), recordId);
        }
        this.anchor = anchor;
    }

    /**
     * The color applied to the lines of this shape.
     */
    public int getLineStyleColor() {
        EscherRGBProperty rgbProperty = _optRecord.lookup(EscherProperties.LINESTYLE__COLOR);
        return rgbProperty == null ? LINESTYLE__COLOR_DEFAULT : rgbProperty.getRgbColor();
    }

    /**
     * The color applied to the lines of this shape.
     */
    public void setLineStyleColor(int lineStyleColor) {
        setPropertyValue(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, lineStyleColor));
    }

    /**
     * The color applied to the lines of this shape.
     */
    public void setLineStyleColor(int red, int green, int blue) {
        int lineStyleColor = ((blue) << 16) | ((green) << 8) | red;
        setPropertyValue(new EscherRGBProperty(EscherProperties.LINESTYLE__COLOR, lineStyleColor));
    }

    /**
     * The color used to fill this shape.
     */
    public int getFillColor() {
        EscherRGBProperty rgbProperty = _optRecord.lookup(EscherProperties.FILL__FILLCOLOR);
        return rgbProperty == null ? FILL__FILLCOLOR_DEFAULT : rgbProperty.getRgbColor();
    }

    /**
     * The color used to fill this shape.
     */
    public void setFillColor(int fillColor) {
        setPropertyValue(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, fillColor));
    }

    /**
     * The color used to fill this shape.
     */
    public void setFillColor(int red, int green, int blue) {
        int fillColor = ((blue) << 16) | ((green) << 8) | red;
        setPropertyValue(new EscherRGBProperty(EscherProperties.FILL__FILLCOLOR, fillColor));
    }

    /**
     * @return returns with width of the line in EMUs.  12700 = 1 pt.
     */
    public int getLineWidth() {
        EscherSimpleProperty property = _optRecord.lookup(EscherProperties.LINESTYLE__LINEWIDTH);
        return property == null ? LINEWIDTH_DEFAULT: property.getPropertyValue();
    }

    /**
     * Sets the width of the line.  12700 = 1 pt.
     *
     * @param lineWidth width in EMU's.  12700EMU's = 1 pt
     * @see HSSFShape#LINEWIDTH_ONE_PT
     */
    public void setLineWidth(int lineWidth) {
        setPropertyValue(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEWIDTH, lineWidth));
    }

    /**
     * @return One of the constants in LINESTYLE_*
     */
    public int getLineStyle() {
        EscherSimpleProperty property = _optRecord.lookup(EscherProperties.LINESTYLE__LINEDASHING);
        if (null == property){
            return LINESTYLE_DEFAULT;
        }
        return property.getPropertyValue();
    }

    /**
     * Sets the line style.
     *
     * @param lineStyle One of the constants in LINESTYLE_*
     */
    public void setLineStyle(int lineStyle) {
        setPropertyValue(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEDASHING, lineStyle));
        if (getLineStyle() != HSSFShape.LINESTYLE_SOLID) {
            setPropertyValue(new EscherSimpleProperty(EscherProperties.LINESTYLE__LINEENDCAPSTYLE, 0));
            if (getLineStyle() == HSSFShape.LINESTYLE_NONE){
                setPropertyValue(new EscherBoolProperty( EscherProperties.LINESTYLE__NOLINEDRAWDASH, 0x00080000));
            } else {
                setPropertyValue( new EscherBoolProperty( EscherProperties.LINESTYLE__NOLINEDRAWDASH, 0x00080008));
            }
        }
    }

    /**
     * @return <code>true</code> if this shape is not filled with a color.
     */
    public boolean isNoFill() {
        EscherBoolProperty property = _optRecord.lookup(EscherProperties.FILL__NOFILLHITTEST);
        return property == null ? NO_FILL_DEFAULT : property.getPropertyValue() == NO_FILLHITTEST_TRUE;
    }

    /**
     * @param noFill sets whether this shape is filled or transparent.
     */
    public void setNoFill(boolean noFill) {
        setPropertyValue(new EscherBoolProperty(EscherProperties.FILL__NOFILLHITTEST, noFill ? NO_FILLHITTEST_TRUE : NO_FILLHITTEST_FALSE));
    }

    protected void setPropertyValue(EscherProperty property){
        _optRecord.setEscherProperty(property);
    }

    /**
     * @param value specifies whether this shape is vertically flipped.
     */
    public void setFlipVertical(boolean value){
        EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
        if (value){
            sp.setFlags(sp.getFlags() | EscherSpRecord.FLAG_FLIPVERT);
        } else {
            sp.setFlags(sp.getFlags() & (Integer.MAX_VALUE - EscherSpRecord.FLAG_FLIPVERT));
        }
    }

    /**
     * @param value specifies whether this shape is horizontally flipped.
     */
    public void setFlipHorizontal(boolean value){
        EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
        if (value){
            sp.setFlags(sp.getFlags() | EscherSpRecord.FLAG_FLIPHORIZ);
        } else {
            sp.setFlags(sp.getFlags() & (Integer.MAX_VALUE - EscherSpRecord.FLAG_FLIPHORIZ));
        }
    }

    /**
     * @return whether this shape is vertically flipped.
     */
    public boolean isFlipVertical(){
        EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
        return (sp.getFlags() & EscherSpRecord.FLAG_FLIPVERT) != 0;
    }

    /**
     * @return whether this shape is horizontally flipped.
     */
    public boolean isFlipHorizontal(){
        EscherSpRecord sp = getEscherContainer().getChildById(EscherSpRecord.RECORD_ID);
        return (sp.getFlags() & EscherSpRecord.FLAG_FLIPHORIZ) != 0;
    }

    /**
     * @return the rotation, in degrees, that is applied to a shape.
     */
    public int getRotationDegree(){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EscherSimpleProperty property = getOptRecord().lookup(EscherProperties.TRANSFORM__ROTATION);
        if (null == property){
            return 0;
        }
        try {
            LittleEndian.putInt(property.getPropertyValue(), bos);
            return LittleEndian.getShort(bos.toByteArray(), 2);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * specifies the rotation, in degrees, that is applied to a shape.
     * Positive values specify rotation in the clockwise direction.
     * Negative values specify rotation in the counterclockwise direction.
     * Rotation occurs around the center of the shape.
     * The default value for this property is 0x00000000
     * @param value
     */
    public void setRotationDegree(short value){
        setPropertyValue(new EscherSimpleProperty(EscherProperties.TRANSFORM__ROTATION , (value << 16)));
    }

    /**
     * Count of all children and their children's children.
     */
    public int countOfAllChildren() {
        return 1;
    }

    protected abstract HSSFShape cloneShape();

    protected void setPatriarch(HSSFPatriarch _patriarch) {
        this._patriarch = _patriarch;
    }

    public HSSFPatriarch getPatriarch() {
        return _patriarch;
    }

    protected void setParent(HSSFShape parent) {
        this.parent = parent;
    }
}
