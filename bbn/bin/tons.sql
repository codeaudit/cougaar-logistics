select a.ownerid, "Tons", round(sum(c.weight)*0.0000011023) from assetinstance_XXX a, cargocatcodedim_XXX c where a.prototypeid not like "MOS%" and a.prototypeid = c.prototypeid group by ownerid;
