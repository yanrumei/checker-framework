# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

DIA?=dia

EPSFILES = fenum.eps guardedby.eps igj.eps initialization.eps interning.eps javari.eps nullness.eps nullness-and-rawness.eps rawness.eps regex.eps signature-types.eps formatter-hierarchy.eps formatter-categories.eps value-hierarchy3.eps value-subtype.eps value-subtyping.eps
PDFFILES = fenum.pdf guardedby.pdf igj.pdf initialization.pdf interning.pdf javari.pdf nullness.pdf nullness-and-rawness.pdf rawness.pdf regex.pdf signature-types.pdf formatter-hierarchy.pdf formatter-categories.pdf value-hierarchy3.pdf value-subtype.pdf value-subtyping.pdf
PNGFILES = fenum.png guardedby.png igj.png initialization.png interning.png javari.png nullness.png nullness-and-rawness.png rawness.png regex.png signature-types.png formatter-hierarchy.png formatter-categories.png value-hierarchy3.png value-subtype.png value-subtyping.png

all: ${EPSFILES} ${PDFFILES} ${PNGFILES} png-copy

png-copy: ${PNGFILES}
	cp -pf ${PNGFILES} ..

%.eps : %.dia
	$(DIA) -t eps $<

%.pdf : %.eps
	epstopdf $<

%.png : %.dia
	$(DIA) -t png $<

# Direct dia translation overlays the "R" and "e" in "ReadOnly".
igj.png: igj.pdf
	convert -geometry 406x224 $< $@
javari.png: javari.pdf
	convert -geometry 406x224 $< $@
# Direct dia translation overlays the "" and vowel in "Fully" and "Field".
signature-types.png: signature-types.pdf
	convert -geometry 406x224 $< $@

clean:
	@\rm *.eps
	@\rm *.pdf
	@\rm *.png
